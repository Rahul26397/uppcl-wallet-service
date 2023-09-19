package com.tequre.wallet.consumer.registration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.data.AgentDocument;
import com.tequre.wallet.data.RegisterAgent;
import com.tequre.wallet.data.User;
import com.tequre.wallet.enums.AgentRegistrationEvents;
import com.tequre.wallet.enums.AgentRegistrationStates;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.DocumentStatus;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.enums.RegisterAgentStatus;
import com.tequre.wallet.enums.Role;
import com.tequre.wallet.enums.UserStatus;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.repository.RegisterAgentRepository;
import com.tequre.wallet.repository.UserRepository;
import com.tequre.wallet.request.AgentRegisterRequest;
import com.tequre.wallet.service.NotificationService;
import com.tequre.wallet.service.UserService;
import com.tequre.wallet.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class AgentRegistrationConsumer {

    private final Logger logger = LoggerFactory.getLogger(AgentRegistrationConsumer.class);

    @Autowired
    private Gson gson;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private StateMachineService<AgentRegistrationStates, AgentRegistrationEvents> stateMachineService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RegisterAgentRepository registerAgentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private UserService userService;

    @StreamListener(AgentRegistrationStream.INBOUND)
    public void consumeEvent(@Payload Event event) {
        logger.info("Agent Registration Consumer: {}", event);
        if (AgentRegisterRequest.class.getSimpleName().equals(event.getType())) {
            Map<String, Object> map = (Map<String, Object>) event.getPayload();
            JsonElement obj = gson.toJsonTree(map);
            AgentRegisterRequest agentRegisterRequest = gson.fromJson(obj, AgentRegisterRequest.class);
            handleAgentRegistrationRequest(event, agentRegisterRequest);
        }
    }

    private void handleAgentRegistrationRequest(Event event, AgentRegisterRequest agentRegisterRequest) {
        try {
            // 1) Create User
            User user = createUser(agentRegisterRequest, agentRegisterRequest.getAgentType() == AgentType.AGENCY ? Role.AGENCY : Role.AGENT);

            // 2) Register Agent
            RegisterAgent registerAgent = publishAgentRegistration(agentRegisterRequest);

            // 3) Initialize StateMachine
            StateMachine<AgentRegistrationStates,
                    AgentRegistrationEvents> stateMachine = getStateMachine(registerAgent.getId(), true);

            // 4) Notify user with email & password
            //String url = integrationConfig.getAdminNode() + "/v1/fsm/event/" +
            // registerAgent.getId() + "/REGISTER";
            String url = integrationConfig.getAdminNode() + "/" +
                    registerAgent.getId() + "/REGISTER";
            Map<String, String> fields = new HashMap<>();
            fields.put("email", user.getEmail());
            fields.put("firstName", user.getFirstName());
            fields.put("lastName", user.getLastName());
            fields.put("userName", user.getUserName());
            fields.put("password", user.getPassword());
            fields.put("url", url);
            notificationService.email("AGENT_REGISTRATION", fields);

            event.setStatus(EventStatus.SUCCESS);
            logger.info("Agent registration successful " + agentRegisterRequest);
        } catch (Throwable th) {
            logger.error("An exception occurred while processing agent registration request.", th);
            event.setStatus(EventStatus.FAILED);
            if (th instanceof HttpStatusCodeException) {
                HttpStatusCodeException e = (HttpStatusCodeException) th;
                int statusCode = e.getStatusCode().value();
                String reason = e.getResponseBodyAsString();
                if (reason == null) {
                    reason = e.getMessage();
                }
                event.setReason("Http Code: " + statusCode + " Response: " + reason);
            } else {
                event.setReason(th.getMessage());
            }
        } finally {
            Optional<Event> eventOptional = eventRepository.findById(event.getId());
            if (eventOptional.isPresent()) {
                Event oldEvent = eventOptional.get();
                if (oldEvent.getStatus() != EventStatus.SUCCESS) {
                    eventRepository.save(event);
                } else {
                    logger.info("Event status already in Success: {}", event);
                }
            }
            logger.info("Event Processed Successful: {}", event);
        }
    }

    private RegisterAgent publishAgentRegistration(AgentRegisterRequest agentRegisterRequest) {
        RegisterAgent registerAgent = new RegisterAgent();
        registerAgent.setId(CommonUtils.generateUUID());
        registerAgent.setFirstName(agentRegisterRequest.getFirstName());
        registerAgent.setLastName(agentRegisterRequest.getLastName());
        registerAgent.setAgentType(agentRegisterRequest.getAgentType());
        registerAgent.setAreaType(agentRegisterRequest.getAreaType());
        registerAgent.setAgencyType(agentRegisterRequest.getAgencyType());
        registerAgent.setSubAgentType(agentRegisterRequest.getSubAgentType());
        registerAgent.setAgencyId(agentRegisterRequest.getAgencyId());
        registerAgent.setAgencyName(agentRegisterRequest.getAgencyName());
        registerAgent.setDiscoms(agentRegisterRequest.getDiscoms());
        registerAgent.setDivisions(agentRegisterRequest.getDivisions());
        registerAgent.setEmail(agentRegisterRequest.getEmail());
        registerAgent.setUserName(agentRegisterRequest.getUserName());
        registerAgent.setMobile(agentRegisterRequest.getMobile());
        registerAgent.setPanNumber(agentRegisterRequest.getPanNumber());
        registerAgent.setGstin(agentRegisterRequest.getGstin());
        registerAgent.setRegistrationNumber(agentRegisterRequest.getRegistrationNumber());
        registerAgent.setTinNumber(agentRegisterRequest.getTinNumber());
        registerAgent.setAddress(agentRegisterRequest.getAddress());
        registerAgent.setResidenceAddress(agentRegisterRequest.getResidenceAddress());
        registerAgent.setStatus(RegisterAgentStatus.NEW);
        registerAgent.setImageUrl(agentRegisterRequest.getImageUrl());
        registerAgent.setAccountNumber(agentRegisterRequest.getAccountNumber());
        registerAgent.setEmpId(agentRegisterRequest.getEmpId());
        registerAgent.setUniqueId(agentRegisterRequest.getUniqueId());
        registerAgent.setIfsc(agentRegisterRequest.getIfsc());
        if (agentRegisterRequest.getDocuments() != null) {
            List<AgentDocument> documents = agentRegisterRequest.getDocuments().stream()
                    .map(documentRequest -> {
                        AgentDocument document = new AgentDocument();
                        document.setLocation(documentRequest.getLocation());
                        document.setType(documentRequest.getType());
                        document.setStatus(DocumentStatus.NEW);
                        return document;
                    })
                    .collect(Collectors.toList());
            registerAgent.setDocuments(documents);
        }
        return registerAgentRepository.save(registerAgent);
    }

    private User createUser(AgentRegisterRequest agentRegisterRequest, Role role) {
        User user = new User();
        user.setId(CommonUtils.generateUUID());
        user.setEmail(agentRegisterRequest.getEmail());
        user.setFirstName(agentRegisterRequest.getFirstName());
        user.setLastName(agentRegisterRequest.getLastName());
        user.setStatus(UserStatus.PENDING);
        user.setMobile(agentRegisterRequest.getMobile());
        user.setUserName(agentRegisterRequest.getUserName());
        user.setPassword(CommonUtils.generatePassword());
        user.setAddress(agentRegisterRequest.getAddress());
        user.setResidenceAddress(agentRegisterRequest.getResidenceAddress());
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

    private synchronized StateMachine<AgentRegistrationStates,
            AgentRegistrationEvents> getStateMachine(String id, boolean start) {
        StateMachine<AgentRegistrationStates, AgentRegistrationEvents> stateMachine = stateMachineService
                .acquireStateMachine(id, start);
        return stateMachine;
    }

}
