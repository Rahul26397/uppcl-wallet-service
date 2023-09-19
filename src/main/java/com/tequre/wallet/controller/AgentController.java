package com.tequre.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.AggregateIterable;
import com.tequre.wallet.config.AgentRegistrationStateMachineConfig;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.AgentDocument;
import com.tequre.wallet.data.AgentUser;
import com.tequre.wallet.data.RegisterAgent;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.data.User;
import com.tequre.wallet.enums.AgentStatus;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.AreaType;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.enums.MeterAgentStatus;
import com.tequre.wallet.enums.RegisterAgentStatus;
import com.tequre.wallet.enums.Role;
import com.tequre.wallet.enums.SubAgentType;
import com.tequre.wallet.enums.SyncStatus;
import com.tequre.wallet.enums.UserStatus;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.AgentUserRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.repository.RegisterAgentRepository;
import com.tequre.wallet.repository.UserRepository;
import com.tequre.wallet.request.AgencyCommissionRateRequest;
import com.tequre.wallet.request.AgencyLimitsRequest;
import com.tequre.wallet.request.AgencyUserCreateRequest;
import com.tequre.wallet.request.AgentRegisterRequest;
import com.tequre.wallet.request.AgentStatusRequest;
import com.tequre.wallet.request.AgentUpdateRequest;
import com.tequre.wallet.request.CreateWalletRequest;
import com.tequre.wallet.request.DocumentStatusRequest;
import com.tequre.wallet.request.MeterAgentStatusUpdateRequest;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.request.RegisterAgentRequest;
import com.tequre.wallet.request.RegisterMeterAgentRequest;
import com.tequre.wallet.request.ValidateMeterAgentRequest;
import com.tequre.wallet.response.AcceptedResponse;
import com.tequre.wallet.response.AgencyDistrictLookupEntry;
import com.tequre.wallet.response.ValidateRuralMeterAgentResponse;
import com.tequre.wallet.response.ValidateUrbanMeterAgentResponse;
import com.tequre.wallet.service.DivisionService;
import com.tequre.wallet.service.NotificationService;
import com.tequre.wallet.service.RuralMeterReaderService;
import com.tequre.wallet.service.StorageService;
import com.tequre.wallet.service.UrbanMeterReaderService;
import com.tequre.wallet.service.UserService;
import com.tequre.wallet.service.WalletService;
import com.tequre.wallet.service.producer.AgentRegistrationStreamService;
import com.tequre.wallet.utils.CommonUtils;
import com.tequre.wallet.utils.Constants;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/agent")
public class AgentController {

    private final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentRegistrationStreamService agentRegistrationStreamService;

    @Autowired
    private RegisterAgentRepository registerAgentRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Gson gson;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UrbanMeterReaderService urbanMeterReaderService;

    @Autowired
    private RuralMeterReaderService ruralMeterReaderService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AgentUserRepository agentUserRepository;
    
    @Autowired
    private DivisionService divisionService;
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private AgentRegistrationStateMachineConfig agentRegistrationStateMachineConfig;

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterAgentRequest registerAgentRequest) {
        AgentRegisterRequest agentRegisterRequest = transform(registerAgentRequest);
        Event event = CommonUtils.createEvent(agentRegisterRequest);
        agentRegistrationStreamService.produceEvent(event);
        AcceptedResponse response = CommonUtils.eventResponse(event.getId());
        return ResponseEntity.accepted().body(response);
    }

    @RequestMapping(value = "/agency/{id}/register/user", method = RequestMethod.POST)
    public ResponseEntity<?> registerUser(@PathVariable String id,
                                          @Valid @RequestBody AgencyUserCreateRequest agencyUserCreateRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            if (agent.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Event event = CommonUtils.createEvent(agencyUserCreateRequest);
            AcceptedResponse response = CommonUtils.eventResponse(event.getId());
            try {
                // Create User
                User user = createUser(agencyUserCreateRequest, Role.AGENCY_REPORT_VIEWER);
                // Create User Agent
                AgentUser agentUser = new AgentUser();
                agentUser.setId(user.getId());
                agentUser.setUser(user);
                agentUser.setVanId(agent.getVan());
                agentUser.setAgentType(AgentType.AGENCY);
                agentUser.setAgentId(id);
                agentUserRepository.save(agentUser);
                event.setStatus(EventStatus.SUCCESS);
                logger.info("User creation successful: {}", agencyUserCreateRequest);

                try {
                    // Send-Notification
                    Map<String, String> fields = new HashMap<>();
                    fields.put("email", user.getEmail());
                    fields.put("firstName", user.getFirstName());
                    fields.put("lastName", user.getLastName());
                    fields.put("userName", user.getUserName());
                    fields.put("password", user.getPassword());
                    fields.put("url", Role.AGENCY_REPORT_VIEWER.name());
                    notificationService.email("AGENT_REGISTRATION", fields);
                } catch (Throwable e) {
                    logger.error("Agent User Notification Failed for agency {} & user {}",
                            agent.getId(), user.getId(), e);
                }
            } catch (Throwable e) {
                event.setStatus(EventStatus.FAILED);
                event.setReason(e.getMessage());
                logger.error("An exception occurred while processing user create request.", e);
            } finally {
                eventRepository.save(event);
                logger.info("Event Processed Successful {}", event);
            }
            return ResponseEntity.accepted().body(response);
        } else {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Invalid agency id " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(serviceMessage);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getAgent(@PathVariable String id) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            if (agent.getAgentType() == AgentType.AGENCY) {
                agent.setTotalAgents(findAgentsForAgency(agent.getId()));
            }
            agent.setBalanceAmount(walletService.getBalance(agent.getVan()));
            return ResponseEntity.status(HttpStatus.OK).body(agent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable String id) {
        agentRepository.deleteById(id);
    }

    @RequestMapping(value = "/{id}/status", method = RequestMethod.PUT)
    public ResponseEntity<?> changeAgentStatus(@PathVariable String id,
                                               @RequestBody AgentStatusRequest agentStatusRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            agent.setStatus(agentStatusRequest.getStatus());
            agent = agentRepository.save(agent);
            return ResponseEntity.status(HttpStatus.OK).body(agent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getAgents(@RequestParam(value = "status", required = false) AgentStatus status,
                                       @RequestParam(value = "agentType", required = false) AgentType agentType,
                                       @RequestParam(value = "areaType", required = false) AreaType areaType,
                                       @RequestParam(value = "subAgentType", required = false) SubAgentType subAgentType,
                                       @RequestParam(value = "district", required = false) String district,
                                       @RequestParam(value = "discom", required = false) String discom,
                                       @RequestParam(value = "division", required = false) String division,
                                       @RequestParam(value = "userId", required = false) String userId,
                                       @RequestParam(value = "agencyId", required = false) String agencyId,
                                       @RequestParam(value = "van", required = false) String van,
                                       @RequestParam(value = "startTime", required = false) Long startTime,
                                       @RequestParam(value = "endTime", required = false) Long endTime,
                                       @RequestParam(value = "pageSize", required = false) String pageSize,
                                       @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (agentType != null) {
            query.addCriteria(Criteria.where("agentType").is(agentType));
        }
        if (discom != null) {
            query.addCriteria(Criteria.where("discoms").regex("^" + discom + "$", "i"));
        }
        if (division != null) {
            query.addCriteria(Criteria.where("divisions").regex("^" + division + "$", "i"));
        }
        if (subAgentType != null) {
            query.addCriteria(Criteria.where("subAgentType").is(subAgentType));
        }
        if (areaType != null) {
            query.addCriteria(Criteria.where("areaType").is(areaType));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (district != null) {
            query.addCriteria(Criteria.where("district").regex("^" + district + "$", "i"));
        }
        if (userId != null) {
            Optional<AgentUser> userOpt = agentUserRepository.findById(userId);
            if (userOpt.isPresent()) {
                AgentUser agentUser = userOpt.get();
                query.addCriteria(Criteria.where("van").is(agentUser.getVanId()));
            } else {
                query.addCriteria(Criteria.where("user.id").is(userId));
            }
        }
        if (van != null) {
            query.addCriteria(Criteria.where("van").is(van));
        }
        if (agencyId != null) {
            query.addCriteria(Criteria.where("agencyId").is(agencyId));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("createdAt");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        query.with(pageableRequest);
        List<Agent> agents = mongoTemplate.find(query, Agent.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (agents != null) {
            // Line below is needed to handle gson serialization.
            agents.forEach(agent -> {
                if (agent.getAgentType() == AgentType.AGENCY) {
                    agent.setAgentsLimit(agent.getAgentsLimit());
                    agent.setTotalAgents(findAgentsForAgency(agent.getId()));
                }
            });
            element = gson.toJsonTree(agents, new TypeToken<List<Agent>>() {
            }.getType());
            JsonArray array = element.getAsJsonArray();
            if (p.getSize() - array.size() == 0) {
                object.addProperty("nextPageToken", CommonUtils.nextPageToken(gson, p));
            }
            object.addProperty("recordCount", array.size());
            object.add("result", array);
        }
        JsonNode jsonNode = CommonUtils.getMapper().readTree(object.toString());
        return ResponseEntity.status(HttpStatus.OK).body(jsonNode);
    }

    @RequestMapping(value = "/agency/{id}/register", method = RequestMethod.POST)
    public ResponseEntity<?> registerAgencyAgent(@PathVariable String id,
                                                 @Valid @RequestBody RegisterAgentRequest registerAgentRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            if (agent.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            long currentAgentsCount = findAgentsForAgency(id);
            if (agent.getAgentsLimit() < currentAgentsCount + 1) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The Agency is not allowed to register more than " +
                        agent.getAgentsLimit() + " agents.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            AgentRegisterRequest agentRegisterRequest = transform(registerAgentRequest);
            agentRegisterRequest.setAgencyId(id);
            agentRegisterRequest.setAgentType(AgentType.AGENT);
            Event event = CommonUtils.createEvent(agentRegisterRequest);
            agentRegistrationStreamService.produceEvent(event);
            AcceptedResponse response = CommonUtils.eventResponse(event.getId());
            return ResponseEntity.accepted().body(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/agency/{id}/register/meter-agent", method = RequestMethod.POST)
    public ResponseEntity<?> registerAgencyMeterAgent(@PathVariable String id,
                                                      @Valid @RequestBody RegisterMeterAgentRequest registerMeterAgentRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            if (agent.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            long currentAgentsCount = findAgentsForAgency(id);
            if (agent.getAgentsLimit() < currentAgentsCount + 1) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The Agency is not allowed to register more than " +
                        agent.getAgentsLimit() + " agents.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            if (!verifyMeterAgentUniqueId(registerMeterAgentRequest.getUniqueId())) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Specified Unique Id " +
                        registerMeterAgentRequest.getUniqueId() + " is already registered.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            if (!isEmailUnique(registerMeterAgentRequest.getEmail())) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Specified Email Id " +
                        registerMeterAgentRequest.getEmail() + " is already registered.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            AgentRegisterRequest agentRegisterRequest = transform(registerMeterAgentRequest);
            agentRegisterRequest.setAgencyId(id);
            agentRegisterRequest.setAgentType(AgentType.AGENT);
            agentRegisterRequest.setSubAgentType(SubAgentType.METER_READER_AGENT);
            Event event = CommonUtils.createEvent(agentRegisterRequest);
            agentRegistrationStreamService.produceEvent(event);
            AcceptedResponse response = CommonUtils.eventResponse(event.getId());
            return ResponseEntity.accepted().body(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    private String generateVan() {
        Agent filterVan = new Agent();
        String van = null;
        do {
            van = CommonUtils.generateVan();
            filterVan.setVan(Constants.UPPCL_ACC + van);
        } while (agentRepository.findAll(Example.of(filterVan)).size() != 0);
        return Constants.UPPCL_ACC + van;
    }
    
    @Deprecated
    @RequestMapping(value = "/register/department", method = RequestMethod.POST)
    public ResponseEntity<?> registerDepartment(@Valid @RequestBody Agent registerAgentRequest) {
    	String van = generateVan();
    	Document document = new Document();
        document.append("agentType", "DEPARTMENT");
        document.append("agencyType", "DEPARTMENT");
        document.append("areaType", "RAPDRP");
	      document.append("status", "ACTIVE");
	      document.append("van", van);
	      // document.append("agencyId", registerAgentRequest.getAgencyId());
	      document.append("agencyName", "DEPARTMENT");
	      // document.append("empId", registerAgentRequest.getEmpId());
	      // document.append("uniqueId", registerAgentRequest.getUniqueId());
	      // document.append("accountNumber", registerAgentRequest.getAccountNumber());
	      // document.append("discom", registerAgentRequest.getDiscoms());
	      // document.append("division", registerAgentRequest.getDivisions());
	      // document.append("ifsc", registerAgentRequest.getIfsc());
	      document.append("district", "AGRA");
	      document.append("syncStatus", "SYNC_COMPLETED");
//        document.append("areaType", registerAgentRequest.getAreaType().name());
//        document.append("status", registerAgentRequest.getStatus());
//        document.append("van", registerAgentRequest.getVan());
//        document.append("agencyId", registerAgentRequest.getAgencyId());
//        document.append("agencyName", registerAgentRequest.getAgencyName());
//        document.append("empId", registerAgentRequest.getEmpId());
//        document.append("uniqueId", registerAgentRequest.getUniqueId());
//        document.append("accountNumber", registerAgentRequest.getAccountNumber());
//        document.append("discom", registerAgentRequest.getDiscoms());
//        document.append("division", registerAgentRequest.getDivisions());
//        document.append("ifsc", registerAgentRequest.getIfsc());
//        document.append("district", registerAgentRequest.getDistrict());
//        document.append("syncStatus", registerAgentRequest.getSyncStatus());
        
        document.append("createdAt", new Date());
        document.append("modifiedAt", new Date());
        String id = CommonUtils.generateUUID();
        document.append("_id", id);
    	storageService.insertOne(document);
    	ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setMessage("User is created successfully for email: ");
     // Create wallet in HLF
        CreateWalletRequest createWalletRequest = new CreateWalletRequest();
        createWalletRequest.setVanId(van);
        createWalletRequest.setWalletStatus("ACTIVE");
        createWalletRequest.setBalance(0d);
        createWalletRequest.setWalletType("DEPARTMENT");
        ResponseEntity<Object> response = walletService.createWallet(createWalletRequest);
        logger.info("Wallet Creation Request Status: " + response.getStatusCode() + " Body: " + response.getBody());
        return ResponseEntity.status(HttpStatus.OK).body(serviceMessage);
    }


    @RequestMapping(value = "/agency/{id}/sync/meter-agent/{agentId}", method = RequestMethod.POST)
    public ResponseEntity<?> syncMeterAgentByAgency(@PathVariable String id, @PathVariable String agentId) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agency = optionalAgent.get();
            if (agency.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Agent queryAgent = new Agent();
            queryAgent.setId(agentId);
            queryAgent.setAgencyId(agency.getId());
            queryAgent.setSubAgentType(SubAgentType.METER_READER_AGENT);
            Optional<Agent> optAgent = agentRepository.findOne(Example.of(queryAgent));
            if (!optAgent.isPresent()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The specified agent '" + agentId + "' is not found associated with agency '" + id + "'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Agent agent = optAgent.get();
            if (agent.getSyncStatus() != SyncStatus.SYNC_PENDING) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent is already synched");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            boolean isSynched = false;
            switch (agent.getAreaType()) {
                case NON_RAPDRP:
                    isSynched = ruralMeterReaderService.sync(agent.getUniqueId(),
                            agent.getDiscoms() != null && !agent.getDiscoms().isEmpty() ? agent.getDiscoms().get(0) : "",
                            agent.getVan(), agent.getId());
                    break;
                case RAPDRP:
                    isSynched = urbanMeterReaderService.sync(agent.getUniqueId(),
                            agency.getId(), agency.getVan(), agent.getId(), agent.getVan(), agent.getDiscoms() != null && !agent.getDiscoms().isEmpty() ? agent.getDiscoms().get(0) : "");
                    break;
                default:
                    ServiceMessage serviceMessage = new ServiceMessage();
                    serviceMessage.setMessage("Unrecognized area type " + agent.getAreaType());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            if (isSynched) {
                agent.setSyncStatus(SyncStatus.SYNC_COMPLETED);
                agentRepository.save(agent);
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Sync operation completed.");
                return ResponseEntity.status(HttpStatus.OK).body(serviceMessage);
            } else {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Sync operation failed. Please retry.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(serviceMessage);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody AgentUpdateRequest agentUpdateRequest) {
        Optional<Agent> agentOptional = agentRepository.findById(id);
        if (agentOptional.isPresent()) {
            Agent agent = agentOptional.get();
            if (agentUpdateRequest.getAccountNumber() != null) {
                agent.setAccountNumber(agentUpdateRequest.getAccountNumber());
            }
            if (agentUpdateRequest.getIfsc() != null) {
                agent.setIfsc(agentUpdateRequest.getIfsc());
            }
            if (!CollectionUtils.isEmpty(agentUpdateRequest.getDiscoms())) {
                agent.setDiscoms(agentUpdateRequest.getDiscoms());
            }
            if (!CollectionUtils.isEmpty(agentUpdateRequest.getDivisions())) {
                agent.setDivisions(agentUpdateRequest.getDivisions());
            }
            if (agentUpdateRequest.getEmpId() != null) {
                agent.setEmpId(agentUpdateRequest.getEmpId());
            }
            return ResponseEntity.ok(agentRepository.save(agent));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/agency/{id}/status/meter-agent/{agentId}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateMeterAgentStatusByAgency(@PathVariable String id,
                                                            @PathVariable String agentId,
                                                            @RequestBody MeterAgentStatusUpdateRequest meterAgentStatusUpdateRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agency = optionalAgent.get();
            if (agency.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Agent queryAgent = new Agent();
            queryAgent.setId(agentId);
            queryAgent.setAgencyId(agency.getId());
            Optional<Agent> optAgent = agentRepository.findOne(Example.of(queryAgent));
            if (!optAgent.isPresent()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The specified agent '" + agentId + "' is not found associated with agency '" + id + "'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Agent agent = optAgent.get();
            AgentStatus status = agent.getStatus();
            MeterAgentStatus meterAgentStatus = meterAgentStatusUpdateRequest.getStatus();
            AgentStatus newStatus = AgentStatus.valueOf(meterAgentStatus.name());
            if (status != newStatus) {
                // Update only if changed
                agent.setStatus(newStatus);
                agent = agentRepository.save(agent);
                if (agency.getStatus() == AgentStatus.ACTIVE) {
                    boolean isSynched = false;
                    switch (agent.getAreaType()) {
                        case NON_RAPDRP:
                            isSynched = ruralMeterReaderService.sync(agent.getUniqueId(),
                                    agent.getDiscoms() != null && agent.getDiscoms().size() > 1 ? agent.getDiscoms().get(0) : "",
                                    agent.getVan(), agency.getId());
                            break;
                        case RAPDRP:
                            isSynched = urbanMeterReaderService.sync(agent.getUniqueId(),
                                    agency.getId(), agency.getVan(), agent.getId(), agent.getVan(),agent.getDiscoms() != null && agent.getDiscoms().size() > 1 ? agent.getDiscoms().get(0) : "");
                            break;
                        default:
                            ServiceMessage serviceMessage = new ServiceMessage();
                            serviceMessage.setMessage("Unrecognized area type " + agent.getAreaType());
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
                    }
                    if (isSynched) {
                        agent.setSyncStatus(SyncStatus.SYNC_COMPLETED);
                        agent = agentRepository.save(agent);
                    }
                } else if (agency.getStatus() == AgentStatus.INACTIVE) {
                    boolean deregisterStatus = false;
                    switch (agent.getAreaType()) {
                        case NON_RAPDRP:
                            deregisterStatus = ruralMeterReaderService.deregister(agent.getUniqueId(),
                                    agent.getDiscoms() != null && agent.getDiscoms().size() > 1 ? agent.getDiscoms().get(0) : "",
                                    agent.getVan(), agency.getId());
                            break;
                        case RAPDRP:
                            deregisterStatus = urbanMeterReaderService.deregister(agent.getUniqueId());
                            break;
                        default:
                            ServiceMessage serviceMessage = new ServiceMessage();
                            serviceMessage.setMessage("Unrecognized area type " + agent.getAreaType());
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
                    }
                    if (!deregisterStatus) {
                        agent.setStatus(status);
                        agentRepository.save(agent);
                        ServiceMessage serviceMessage = new ServiceMessage();
                        serviceMessage.setMessage("Unable to set status to INACTIVE for '" + agentId + "'");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
                    }
                }
            }
            return ResponseEntity.status(HttpStatus.OK).body(agent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/validate/meter-agent", method = RequestMethod.POST)
    public ResponseEntity<?> validateMeterAgent(@Valid @RequestBody ValidateMeterAgentRequest validateMeterAgentRequest) {
        switch (validateMeterAgentRequest.getAreaType()) {
            case NON_RAPDRP:
                return validateRural(validateMeterAgentRequest);
            case RAPDRP:
                return validateUrban(validateMeterAgentRequest);
            default:
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Unrecognized area type " + validateMeterAgentRequest.getAreaType());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
    }

    @RequestMapping(value = "/request/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getRequest(@PathVariable String id) {
        Optional<RegisterAgent> optionalRegisterAgent = registerAgentRepository.findById(id);
        if (optionalRegisterAgent.isPresent()) {
            RegisterAgent registerAgent = optionalRegisterAgent.get();
            return ResponseEntity.status(HttpStatus.OK).body(registerAgent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/request/{id}/document", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDocumentStatus(@PathVariable String id, @RequestBody DocumentStatusRequest documentStatusRequest) {
        Optional<RegisterAgent> optionalRegisterAgent = registerAgentRepository.findById(id);
        RegisterAgent registerAgent = null;
        if (optionalRegisterAgent.isPresent()) {
            registerAgent = optionalRegisterAgent.get();
            if (registerAgent.getDocuments() != null) {
                List<AgentDocument> documents = registerAgent.getDocuments().stream()
                        .map(agentDocument -> {
                            if (agentDocument.getType() == documentStatusRequest.getType()) {
                                agentDocument.setStatus(documentStatusRequest.getStatus());
                            }
                            return agentDocument;
                        }).collect(Collectors.toList());
                registerAgent.setDocuments(documents);
                registerAgentRepository.save(registerAgent);
            }
        }
    }

    @RequestMapping(value = "/request/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequest(@PathVariable String id) {
        registerAgentRepository.deleteById(id);
    }

    @RequestMapping(value = "/request", method = RequestMethod.GET)
    public ResponseEntity<?> getRequests(@RequestParam(value = "agentType", required = false) AgentType agentType,
                                         @RequestParam(value = "subAgentType", required = false) SubAgentType subAgentType,
                                         @RequestParam(value = "areaType", required = false) AreaType areaType,
                                         @RequestParam(value = "status", required = false) RegisterAgentStatus status,
                                         @RequestParam(value = "assignee", required = false) String assignee,
                                         @RequestParam(value = "userName", required = false) String userName,
                                         @RequestParam(value = "startTime", required = false) Long startTime,
                                         @RequestParam(value = "endTime", required = false) Long endTime,
                                         @RequestParam(value = "pageSize", required = false) String pageSize,
                                         @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {

        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (agentType != null) {
            query.addCriteria(Criteria.where("agentType").is(agentType));
        }
        if (subAgentType != null) {
            query.addCriteria(Criteria.where("subAgentType").is(subAgentType));
        }
        if (areaType != null) {
            query.addCriteria(Criteria.where("areaType").is(areaType));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (assignee != null) {
            query.addCriteria(Criteria.where("assignee").is(assignee));
        }
        if (userName != null) {
            query.addCriteria(Criteria.where("userName").is(userName));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("createdAt");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        query.with(pageableRequest);
        List<RegisterAgent> registerAgents = mongoTemplate.find(query, RegisterAgent.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (registerAgents != null) {
            element = gson.toJsonTree(registerAgents, new TypeToken<List<RegisterAgent>>() {
            }.getType());
            JsonArray array = element.getAsJsonArray();
            if (p.getSize() - array.size() == 0) {
                object.addProperty("nextPageToken", CommonUtils.nextPageToken(gson, p));
            }
            object.addProperty("recordCount", array.size());
            object.add("result", array);
        }
        JsonNode jsonNode = CommonUtils.getMapper().readTree(object.toString());
        return ResponseEntity.status(HttpStatus.OK).body(jsonNode);
    }

    @RequestMapping(value = "/agency/{id}/limits", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> setLimitsForAgency(@PathVariable String id,
                                                @RequestBody AgencyLimitsRequest agencyLimitsRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            if (agent.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            long currentAgentsCount = findAgentsForAgency(id);
            if (currentAgentsCount > agencyLimitsRequest.getAgentsLimit()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Current number of agents " + currentAgentsCount +
                        " is more than agents limit value " + agencyLimitsRequest.getAgentsLimit() +
                        " for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            agent.setAgentsLimit(agencyLimitsRequest.getAgentsLimit());
            agent = agentRepository.save(agent);
            return ResponseEntity.status(HttpStatus.OK).body(agent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @RequestMapping(value = "/agency/{id}/commissionRate", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> setCommissionRateForAgency(@PathVariable String id,
                                                        @RequestBody AgencyCommissionRateRequest agencyCommissionRateRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            Agent agent = optionalAgent.get();
            if (agent.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("The agent type is not Agency for id " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Double rate = agencyCommissionRateRequest.getCommissionRate();
            if (rate != null && rate < 0d && rate > 100d) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Invalid commission rate in input " + rate);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            // Set commission rate for agency.
            agent.setCommissionRate(agencyCommissionRateRequest.getCommissionRate());
            Double gstRate = agencyCommissionRateRequest.getGstRate();
            if (gstRate != null && gstRate < 0d && gstRate > 100d) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Invalid GST rate in input " + gstRate);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            agent.setGstRate(agencyCommissionRateRequest.getGstRate());
            Double tdsRate = agencyCommissionRateRequest.getTdsRate();
            if (tdsRate != null && tdsRate < 0d && tdsRate > 100d) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Invalid TDS rate in input " + tdsRate);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            agent.setTdsRate(agencyCommissionRateRequest.getTdsRate());
            Double gstTdsRate = agencyCommissionRateRequest.getGstTdsRate();
            if (gstTdsRate != null && gstTdsRate < 0d && gstTdsRate > 100d) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("Invalid GST on TDS rate in input " + gstTdsRate);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            agent.setGstTdsRate(agencyCommissionRateRequest.getGstTdsRate());
            agent = agentRepository.save(agent);
            return ResponseEntity.status(HttpStatus.OK).body(agent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private long findAgentsForAgency(String agencyId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("agentType").is(AgentType.AGENT));
        query.addCriteria(Criteria.where("agencyId").is(agencyId));
        return mongoTemplate.count(query, Agent.class);
    }

    private AgentRegisterRequest transform(RegisterAgentRequest registerAgentRequest) {
        AgentRegisterRequest agentRegisterRequest = new AgentRegisterRequest();
        agentRegisterRequest.setAreaType(registerAgentRequest.getAreaType());
        agentRegisterRequest.setAgentType(registerAgentRequest.getAgentType());
        agentRegisterRequest.setAgencyType(registerAgentRequest.getAgencyType());
        agentRegisterRequest.setFirstName(registerAgentRequest.getFirstName());
        agentRegisterRequest.setLastName(registerAgentRequest.getLastName());
        agentRegisterRequest.setAgencyId(registerAgentRequest.getAgencyId());
        agentRegisterRequest.setAgencyName(registerAgentRequest.getAgencyName());
        agentRegisterRequest.setEmpId(registerAgentRequest.getEmpId());
        agentRegisterRequest.setAccountNumber(registerAgentRequest.getAccountNumber());
        agentRegisterRequest.setIfsc(registerAgentRequest.getIfsc());
        agentRegisterRequest.setDiscoms(registerAgentRequest.getDiscoms());
        agentRegisterRequest.setDivisions(registerAgentRequest.getDivisions());
        agentRegisterRequest.setEmail(registerAgentRequest.getEmail());
        agentRegisterRequest.setUserName(registerAgentRequest.getUserName());
        agentRegisterRequest.setMobile(registerAgentRequest.getMobile());
        agentRegisterRequest.setAddress(registerAgentRequest.getAddress());
        agentRegisterRequest.setResidenceAddress(registerAgentRequest.getResidenceAddress());
        agentRegisterRequest.setRegistrationNumber(registerAgentRequest.getRegistrationNumber());
        agentRegisterRequest.setGstin(registerAgentRequest.getGstin());
        agentRegisterRequest.setPanNumber(registerAgentRequest.getPanNumber());
        agentRegisterRequest.setTinNumber(registerAgentRequest.getTinNumber());
        agentRegisterRequest.setDocuments(registerAgentRequest.getDocuments());
        agentRegisterRequest.setImageUrl(registerAgentRequest.getImageUrl());
        return agentRegisterRequest;
    }

    private AgentRegisterRequest transform(RegisterMeterAgentRequest registerMeterAgentRequest) {
        AgentRegisterRequest agentRegisterRequest = new AgentRegisterRequest();
        agentRegisterRequest.setAreaType(registerMeterAgentRequest.getAreaType());
        agentRegisterRequest.setFirstName(registerMeterAgentRequest.getFirstName());
        agentRegisterRequest.setLastName(registerMeterAgentRequest.getLastName());
        agentRegisterRequest.setAgencyId(registerMeterAgentRequest.getAgencyId());
        agentRegisterRequest.setAgencyName(registerMeterAgentRequest.getAgencyName());
        agentRegisterRequest.setEmpId(registerMeterAgentRequest.getEmpId());
        agentRegisterRequest.setUniqueId(registerMeterAgentRequest.getUniqueId());
        agentRegisterRequest.setAccountNumber(registerMeterAgentRequest.getAccountNumber());
        agentRegisterRequest.setIfsc(registerMeterAgentRequest.getIfsc());
        agentRegisterRequest.setDiscoms(registerMeterAgentRequest.getDiscoms());
        agentRegisterRequest.setDivisions(registerMeterAgentRequest.getDivisions());
        agentRegisterRequest.setEmail(registerMeterAgentRequest.getEmail());
        agentRegisterRequest.setUserName(registerMeterAgentRequest.getUserName());
        agentRegisterRequest.setMobile(registerMeterAgentRequest.getMobile());
        agentRegisterRequest.setAddress(registerMeterAgentRequest.getAddress());
        agentRegisterRequest.setResidenceAddress(registerMeterAgentRequest.getResidenceAddress());
        agentRegisterRequest.setRegistrationNumber(registerMeterAgentRequest.getRegistrationNumber());
        agentRegisterRequest.setGstin(registerMeterAgentRequest.getGstin());
        agentRegisterRequest.setPanNumber(registerMeterAgentRequest.getPanNumber());
        agentRegisterRequest.setTinNumber(registerMeterAgentRequest.getTinNumber());
        agentRegisterRequest.setDocuments(registerMeterAgentRequest.getDocuments());
        agentRegisterRequest.setImageUrl(registerMeterAgentRequest.getImageUrl());
        return agentRegisterRequest;
    }

    private boolean verifyMeterAgentUniqueId(String uniqueId) {
        Agent agent = new Agent();
        agent.setUniqueId(uniqueId);
        return !agentRepository.exists(Example.of(agent));
    }

    private Optional<Agent> getAgentByUniqueId(String empId) {
        Agent agent = new Agent();
        agent.setEmpId(empId);
        return agentRepository.findOne(Example.of(agent));
    }

    private boolean isEmailUnique(String email) {
        User user = new User();
        user.setEmail(email);
        return !userRepository.exists(Example.of(user));
    }

    private ResponseEntity<?> validateUrban(ValidateMeterAgentRequest validateMeterAgentRequest) {
        ResponseEntity<String> response = urbanMeterReaderService.validate(validateMeterAgentRequest.getUniqueId(), validateMeterAgentRequest.getDiscom());
        if (response == null || response.getStatusCode().isError()) {
            ServiceMessage message = new ServiceMessage();
            message.setMessage("Validation Failed for uniqueId " + validateMeterAgentRequest.getUniqueId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
        String body = response.getBody();
        JsonObject validateResponse = gson.fromJson(body, JsonObject.class);
        String responseMessage = validateResponse.get("message").getAsString();
        String responseCode = validateResponse.get("responseCode").getAsString();
        int code = Integer.parseInt(responseCode);
        if (code == 0 && "Valid".equalsIgnoreCase(responseMessage)) {
            String agentName = validateResponse.get("agentName").getAsString();
            String mobileNo = validateResponse.get("mobileNo").getAsString();
            String emailId = validateResponse.get("emailId").getAsString();
            String agencyName = validateResponse.get("agencyName").getAsString();
            String division = validateResponse.get("division").getAsString();
            ValidateUrbanMeterAgentResponse validateUrbanMeterAgentResponse = new ValidateUrbanMeterAgentResponse();
            validateUrbanMeterAgentResponse.setAgentName(agentName);
            validateUrbanMeterAgentResponse.setMobile(mobileNo);
            validateUrbanMeterAgentResponse.setEmail(emailId);
            validateUrbanMeterAgentResponse.setAgencyName(agencyName);
            validateUrbanMeterAgentResponse.setDivision(division);
            Optional<Agent> agentOpt = getAgentByUniqueId(validateMeterAgentRequest.getUniqueId());
            if (agentOpt.isPresent()) {
                Agent agent = agentOpt.get();
                validateUrbanMeterAgentResponse.setAgentId(agent.getId());
                validateUrbanMeterAgentResponse.setAgentVan(agent.getVan());
                validateUrbanMeterAgentResponse.setAreaType(agent.getAreaType());
                validateUrbanMeterAgentResponse.setAgencyId(agent.getAgencyId());
            }
            return ResponseEntity.ok(validateUrbanMeterAgentResponse);
        } else if (code == 0 && "Invalid".equalsIgnoreCase(responseMessage)) {
            ServiceMessage message = new ServiceMessage();
            message.setMessage("Invalid uniqueId " + validateMeterAgentRequest.getUniqueId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
        } else {
            ServiceMessage message = new ServiceMessage();
            message.setMessage("Validation Failed for uniqueId " + validateMeterAgentRequest.getUniqueId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
    }

    private ResponseEntity<?> validateRural(ValidateMeterAgentRequest validateMeterAgentRequest) {
        ResponseEntity<String> response = ruralMeterReaderService.validate(validateMeterAgentRequest.getUniqueId(),
                validateMeterAgentRequest.getDiscom());
        if (response == null || response.getStatusCode().isError()) {
            ServiceMessage message = new ServiceMessage();
            message.setMessage("Validation Failed for uniqueId " + validateMeterAgentRequest.getUniqueId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
        String body = response.getBody();
        JsonElement validateResponse = gson.fromJson(body, JsonElement.class);
        if (validateResponse.isJsonArray()) {
            JsonArray jsonArray = validateResponse.getAsJsonArray();
            JsonObject childResponse = jsonArray.get(0).getAsJsonObject();
            String status = childResponse.get("Status").getAsString();
            if ("Success".equalsIgnoreCase(status)) {
                ValidateRuralMeterAgentResponse validateRuralMeterAgentResponse = new ValidateRuralMeterAgentResponse();
                String firstName = childResponse.get("FirstName").getAsString();
                String lastName = childResponse.get("LastName").getAsString();
                String discom = childResponse.get("discom").getAsString();
                validateRuralMeterAgentResponse.setFirstName(firstName);
                validateRuralMeterAgentResponse.setLastName(lastName);
                validateRuralMeterAgentResponse.setDiscom(discom);
                Optional<Agent> agentOpt = getAgentByUniqueId(validateMeterAgentRequest.getUniqueId());
                if (agentOpt.isPresent()) {
                    Agent agent = agentOpt.get();
                    validateRuralMeterAgentResponse.setAgentId(agent.getId());
                    validateRuralMeterAgentResponse.setAgentVan(agent.getVan());
                    validateRuralMeterAgentResponse.setAreaType(agent.getAreaType());
                    validateRuralMeterAgentResponse.setAgencyId(agent.getAgencyId());
                }
                return ResponseEntity.ok(validateRuralMeterAgentResponse);
            } else {
                ServiceMessage message = new ServiceMessage();
                message.setMessage("Invalid uniqueId " + validateMeterAgentRequest.getUniqueId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
            }
        } else {
            ServiceMessage message = new ServiceMessage();
            message.setMessage("Validation Failed for uniqueId " + validateMeterAgentRequest.getUniqueId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
    }

    @RequestMapping(value = "/addDistrict", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Deprecated
    public void addDistrictToAgents() {
        Query query = new Query();
        query.addCriteria(Criteria.where("district").is(null));
        List<Agent> agents = mongoTemplate.find(query, Agent.class);
        logger.info("Agents with missing district {}", agents.size());
        int counter = 0;
        for (Agent agent : agents) {
            String district = agent.getUser().getAddress().getDistrict();
            agent.setDistrict(district);
            agentRepository.save(agent);
            counter++;
        }
        logger.info("Agents updated with district {}", counter);
    }

    @RequestMapping(value = "/lookup/agency/district", method = RequestMethod.GET)
    public ResponseEntity<?> lookupAgencyByDistrict(@RequestParam(value = "district", required = false) String district,
                                                    @RequestParam(value = "agencyType", required = false) String agencyType) {
        Query query = new Query();
        query.fields().include("id").include("van").include("agencyName");
        query.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY));
        if (agencyType != null) {
            query.addCriteria(Criteria.where("agencyType").regex("^" + agencyType + "$", "i"));
        }
        if (district != null) {
            query.addCriteria(Criteria.where("district").regex("^" + district + "$", "i"));
        }
        List<Agent> agents = mongoTemplate.find(query, Agent.class);
        List<AgencyDistrictLookupEntry> response = new ArrayList<>();
        agents.forEach(agent -> {
            AgencyDistrictLookupEntry entry = new AgencyDistrictLookupEntry();
            entry.setAgencyId(agent.getId());
            entry.setVan(agent.getVan());
            entry.setAgencyName(agent.getAgencyName());
            response.add(entry);
        });
        return ResponseEntity.ok(response);
    }

    private User createUser(AgencyUserCreateRequest agentRegisterRequest, Role role) {
        User user = new User();
        user.setId(CommonUtils.generateUUID());
        user.setEmail(agentRegisterRequest.getEmail());
        user.setFirstName(agentRegisterRequest.getFirstName());
        user.setLastName(agentRegisterRequest.getLastName());
        user.setStatus(UserStatus.ACTIVE);
        user.setMobile(agentRegisterRequest.getMobile());
        user.setUserName(agentRegisterRequest.getUserName());
        user.setPassword(CommonUtils.generatePassword());
        user.setAddress(agentRegisterRequest.getAddress());
        user.setResidenceAddress(agentRegisterRequest.getResidenceAddress());
        logger.info("ROLE: "+role);
        user.setRole(role);
        user.setImageUrl(agentRegisterRequest.getImageUrl());
        User created = userRepository.save(user);
        try {
            userService.createUser(created.getUserName(), created.getPassword(), created.getEmail(), role);
        } catch (Throwable th) {
            userRepository.deleteById(user.getId());
            logger.error("Unable to create auth user " + created.getUserName(), th);
            throw th;
        }
        return created;
    }
    @RequestMapping(value = "/agency/report/viewer",method = RequestMethod.GET)
    public ResponseEntity<?> getAgencyReportViewer(@RequestParam(value = "status", required = false) AgentStatus status,                        
                                       @RequestParam(value = "userId", required = false) String userId,
                                       @RequestParam(value = "mobile", required = false) String mobile,
                                       @RequestParam(value = "agencyId", required = false) String agencyId,
                                       @RequestParam(value = "agencyVan", required = false) String agencyVan,
                                       @RequestParam(value = "startTime", required = false) Long startTime,
                                       @RequestParam(value = "endTime", required = false) Long endTime,
                                       @RequestParam(value = "pageSize", required = false) String pageSize,
                                       @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query(); 
//        query.addCriteria(Criteria.where("role").is(Role.AGENCY_REPORT_VIEWER.name()));        
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (userId != null) {
        	query.addCriteria(Criteria.where("user.id").is(userId));
        }          
        if (agencyVan != null) {
            query.addCriteria(Criteria.where("vanId").is(agencyVan));
        }
        if (agencyId != null) {
            query.addCriteria(Criteria.where("agentId").is(agencyId));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("createdAt");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        query.with(pageableRequest);
        List<AgentUser> agentsUser = mongoTemplate.find(query, AgentUser.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        List<AgentUser> reportViewer = null;
        if (agentsUser != null) {   
        	if (mobile != null) {
        		reportViewer = agentsUser.stream()
        			           .filter(b -> b.getUser().getMobile().equals(mobile))
        			           .filter(b -> b.getUser().getRole().equals(Role.AGENCY_REPORT_VIEWER))
        			           .collect(Collectors.toList());
            }  
			if (reportViewer != null) {
				element = gson.toJsonTree(reportViewer, new TypeToken<List<Agent>>() {
				}.getType());
			} else {
				element = gson.toJsonTree(agentsUser, new TypeToken<List<Agent>>() {
				}.getType());
			}
            JsonArray array = element.getAsJsonArray();
            if (p.getSize() - array.size() == 0) {
                object.addProperty("nextPageToken", CommonUtils.nextPageToken(gson, p));
            }
            object.addProperty("recordCount", array.size());
            object.add("result", array);
        }
        JsonNode jsonNode = CommonUtils.getMapper().readTree(object.toString());
        return ResponseEntity.status(HttpStatus.OK).body(jsonNode);
    }
    
    //@Deprecated
    /*@RequestMapping(value = "/departments/list", method = RequestMethod.GET)
    public ResponseEntity<?> getDepartmentListingBackup(
//    														@RequestParam(value = "discom", required = false) String discom,
//                                                          @RequestParam(value = "division", required = false) String division,
//                                                          @RequestParam(value = "van", required = false) String van,
//                                                          @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
//                                                          @RequestParam(value = "consumerId", required = false) String consumerId,
//                                                          @RequestParam(value = "startTime", required = false) Long startTime,
//                                                          @RequestParam(value = "tillDateStartTime", required = false) Long tillDateStartTime,
//                                                          @RequestParam(value = "endTime", required = false) Long endTime
                            ) throws ParseException {

        Document filter = new Document();
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = (AggregateIterable<Document>) divisionService.getDepartmentListing();
        for (Document dbObject : result) {
            if(dbObject.get("discom") != null) {
                dbObject.put("discom", dbObject.get("discom").toString().toUpperCase());
            }
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }*/
    
    @RequestMapping(value = "/departments/list", method = RequestMethod.GET)
    public ResponseEntity<?> getDepartmentListing(
//    														@RequestParam(value = "discom", required = false) String discom,
//                                                          @RequestParam(value = "division", required = false) String division,
//                                                          @RequestParam(value = "van", required = false) String van,
//                                                          @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
//                                                          @RequestParam(value = "consumerId", required = false) String consumerId,
//                                                          @RequestParam(value = "startTime", required = false) Long startTime,
//                                                          @RequestParam(value = "tillDateStartTime", required = false) Long tillDateStartTime,
//                                                          @RequestParam(value = "endTime", required = false) Long endTime
                            ) throws ParseException {

        Document filter = new Document();
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = (AggregateIterable<Document>) divisionService.getDepartmentListing();
        for (Document dbObject : result) {
            if(dbObject.get("discom") != null) {
                dbObject.put("discom", dbObject.get("discom").toString().toUpperCase());
            }
            logger.info((String) dbObject.get("departmentCode"));
            if(dbObject.get("departmentCode").equals(null) || dbObject.get("departmentCode").equals("")) {
            	dbObject.put("totalCount", "0");
            } else {
            	dbObject.put("totalCount", divisionService.getConnectionCount((String) dbObject.get("departmentCode")));
            }
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }
    
    @RequestMapping(value = "/towers/list", method = RequestMethod.GET)
    public ResponseEntity<?> getTowersListing(
//    														@RequestParam(value = "discom", required = false) String discom,
//                                                          @RequestParam(value = "division", required = false) String division,
//                                                          @RequestParam(value = "van", required = false) String van,
//                                                          @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
//                                                          @RequestParam(value = "consumerId", required = false) String consumerId,
//                                                          @RequestParam(value = "startTime", required = false) Long startTime,
//                                                          @RequestParam(value = "tillDateStartTime", required = false) Long tillDateStartTime,
//                                                          @RequestParam(value = "endTime", required = false) Long endTime
                            ) throws ParseException {

        Document filter = new Document();
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = (AggregateIterable<Document>) divisionService.getTowerListing();
        for (Document dbObject : result) {
            if(dbObject.get("discom") != null) {
                dbObject.put("discom", dbObject.get("discom").toString().toUpperCase());
            }
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }
}
