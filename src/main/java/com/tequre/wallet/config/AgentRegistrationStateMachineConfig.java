package com.tequre.wallet.config;

import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.RegisterAgent;
import com.tequre.wallet.data.User;
import com.tequre.wallet.enums.AgentRegistrationEvents;
import com.tequre.wallet.enums.AgentRegistrationStates;
import com.tequre.wallet.enums.AgentStatus;
import com.tequre.wallet.enums.RegisterAgentStatus;
import com.tequre.wallet.enums.Role;
import com.tequre.wallet.enums.SubAgentType;
import com.tequre.wallet.enums.SyncStatus;
import com.tequre.wallet.enums.UserStatus;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.AgentStateMachineRepository;
import com.tequre.wallet.repository.RegisterAgentRepository;
import com.tequre.wallet.repository.UserRepository;
import com.tequre.wallet.request.CreateWalletRequest;
import com.tequre.wallet.service.NotificationService;
import com.tequre.wallet.service.RuralMeterReaderService;
import com.tequre.wallet.service.UrbanMeterReaderService;
import com.tequre.wallet.service.UserService;
import com.tequre.wallet.service.WalletService;
import com.tequre.wallet.utils.CommonUtils;
import com.tequre.wallet.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Example;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.StateMachineService;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableStateMachineFactory(contextEvents = false)
public class AgentRegistrationStateMachineConfig
        extends EnumStateMachineConfigurerAdapter<AgentRegistrationStates, AgentRegistrationEvents> {

    private final Logger logger = LoggerFactory.getLogger(AgentRegistrationStateMachineConfig.class);

    @Autowired
    private RegisterAgentRepository registerAgentRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private StateMachineRuntimePersister<AgentRegistrationStates,
            AgentRegistrationEvents, String> stateMachineRuntimePersister;

    @Autowired
    @Lazy
    private StateMachineService<AgentRegistrationStates, AgentRegistrationEvents> stateMachineService;

    @Autowired
    private AgentStateMachineRepository agentStateMachineRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UrbanMeterReaderService urbanMeterReaderService;

    @Autowired
    private RuralMeterReaderService ruralMeterReaderService;

    @Autowired
    private UserService userService;

    @Override
    public void configure(StateMachineConfigurationConfigurer<AgentRegistrationStates,
            AgentRegistrationEvents> config) throws Exception {
        config
                .withPersistence()
                .runtimePersister(stateMachineRuntimePersister);
    }

    @Override
    public void configure(StateMachineStateConfigurer<AgentRegistrationStates, AgentRegistrationEvents> states)
            throws Exception {
        states
                .withStates()
                .initial(AgentRegistrationStates.SUBMITTED, executeSubmitted())
                .state(AgentRegistrationStates.FIRST_LEVEL_APPROVAL)
                .state(AgentRegistrationStates.SECOND_LEVEL_APPROVAL)
                .state(AgentRegistrationStates.PENDING)
                .state(AgentRegistrationStates.APPROVED, cleanUp())
                .state(AgentRegistrationStates.REJECTED, cleanUp())
                .states(EnumSet.allOf(AgentRegistrationStates.class));
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> executeSubmitted() {
        return (context) -> {
            String id = context.getStateMachine().getId();
            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(id);
            if (registerAgentOptional.isPresent()) {
                RegisterAgent registerAgent = registerAgentOptional.get();
                if (registerAgent.getAgencyId() != null &&
                        registerAgent.getSubAgentType() == SubAgentType.METER_READER_AGENT) {
                    executeApproval(context);
                } else {
                	if(registerAgent.getAgencyType() == "DEPARTMENT" || registerAgent.getAgencyType() == "TOWER") {
                		executeApproval(context);
                	} else {
                		registerAgent.setStatus(RegisterAgentStatus.PENDING_MAIL_VERIFICATION);
                        registerAgentRepository.save(registerAgent);
                	}
                }
            }
        };
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> cleanUp() {
        return (context) -> {
            stateMachineService.releaseStateMachine(context.getStateMachine().getId());
            agentStateMachineRepository.deleteById(context.getStateMachine().getId());
        };
    }

    // @SuppressWarnings("unlikely-arg-type")
	@Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> activateUser() {
        return (context) -> {
            org.springframework.statemachine.StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = context.getStateMachine();

            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(machine.getId());
            if (registerAgentOptional.isPresent()) {
                RegisterAgent registerAgent = registerAgentOptional.get();
                User exampleUser = new User();
                exampleUser.setEmail(registerAgent.getEmail());
                Optional<User> userOpt = userRepository.findOne(Example.of(exampleUser));
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setStatus(UserStatus.ACTIVE);
                    if(registerAgent.getAgentType().name() == "DEPARTMENT") {
                    	user.setRole(Role.DEPARTMENT);
                    	user.setEmpId(registerAgent.getEmpId());
                    }
                    if(registerAgent.getAgentType().name() == "TOWER") {
                    	user.setRole(Role.TOWER);
                    	user.setEmpId(registerAgent.getEmpId());
                    }
                    userRepository.save(user);
                }
            }
        };
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> executeApproved() {
        return (context) -> {
            executeApproval(context);
        };
    }

    private void executeApproval(StateContext<AgentRegistrationStates, AgentRegistrationEvents> context) {
        StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = context.getStateMachine();
        Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(machine.getId());

        if (!registerAgentOptional.isPresent()) {
            return;
        }

        RegisterAgent registerAgent = registerAgentOptional.get();
        User exampleUser = new User();
        exampleUser.setEmail(registerAgent.getEmail());
        Optional<User> userOpt = userRepository.findOne(Example.of(exampleUser));
        if (!userOpt.isPresent()) {
            return;
        }

        User user = userOpt.get();
        machine.getExtendedState().getVariables().put(Constants.USER_ID, user.getId());

        String van = generateVan();

        // Create wallet in HLF
        CreateWalletRequest createWalletRequest = new CreateWalletRequest();
        createWalletRequest.setVanId(van);
        createWalletRequest.setWalletStatus("ACTIVE");
        createWalletRequest.setBalance(0d);
        createWalletRequest.setWalletType(registerAgent.getAgentType().name());
        ResponseEntity<Object> response = walletService.createWallet(createWalletRequest);
        logger.info("Wallet Creation Request Status: " + response.getStatusCode() + " Body: " + response.getBody());

        Agent createdAgent = createAgent(van, registerAgent, user);

        registerAgent.setStatus(RegisterAgentStatus.FULFILLED);
        registerAgent.setAssignee(null);
        registerAgentRepository.save(registerAgent);

        // Sync meter agent
        if (createdAgent.getSubAgentType() == SubAgentType.METER_READER_AGENT) {
            boolean isSynched = false;
            switch (createdAgent.getAreaType()) {
                case NON_RAPDRP:
                    isSynched = ruralMeterReaderService.sync(createdAgent.getUniqueId(),
                            createdAgent.getDiscoms() != null && !createdAgent.getDiscoms().isEmpty() ? createdAgent.getDiscoms().get(0) : "",
                            createdAgent.getVan(), createdAgent.getId());
                    break;
                case RAPDRP:
                    isSynched = urbanMeterReaderService.sync(createdAgent.getUniqueId(),
                            createdAgent.getId(), createdAgent.getVan(), createdAgent.getId(), createdAgent.getVan(), createdAgent.getDiscoms() != null && !createdAgent.getDiscoms().isEmpty() ? createdAgent.getDiscoms().get(0) : "");
                    break;
            }
            if (isSynched) {
                createdAgent.setSyncStatus(SyncStatus.SYNC_COMPLETED);
                agentRepository.save(createdAgent);
            }
        }

        // Send SMS
        if (user.getMobile() != null) {
            try {
                Map<String, String> fields = new HashMap<>();
                fields.put("phoneNo", user.getMobile());
                fields.put("firstName", user.getFirstName());
                fields.put("lastName", user.getLastName());
                notificationService.sms("REGISTRATION_APPROVAL", fields);
            } catch (Throwable th) {
                logger.warn("Unable to deliver message for request " + registerAgent.getId() + " username " +
                        registerAgent.getEmail());
            }
        }
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> executeRejected() {
        return (context) -> {
            StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = context.getStateMachine();

            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(machine.getId());

            if (!registerAgentOptional.isPresent()) {
                return;
            }
            RegisterAgent registerAgent = registerAgentOptional.get();
            User exampleUser = new User();
            exampleUser.setEmail(registerAgent.getEmail());
            Optional<User> userOpt = userRepository.findOne(Example.of(exampleUser));
            if (!userOpt.isPresent()) {
                return;
            }

            User user = userOpt.get();
            user.setStatus(UserStatus.DELETED);
            userRepository.save(user);

            try {
                userService.deleteUser(user.getUserName());
                // Delete user from local db
                userRepository.deleteById(user.getId());
            } catch (Throwable th) {
                logger.error("Unable to delete auth user {}", user.getUserName(), th);
            }

            registerAgent.setStatus(RegisterAgentStatus.REJECTED);
            registerAgent.setAssignee(null);
            registerAgentRepository.save(registerAgent);
        };
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> assignFirstLevelApproval() {
        return ctx -> {
            String id = ctx.getStateMachine().getId();
            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(id);
            if (!registerAgentOptional.isPresent()) {
                return;
            }
            RegisterAgent registerAgent = registerAgentOptional.get();
            if (registerAgent.getAgencyId() != null) {
                // auto-approval process for agency.
                executeApproval(ctx);
            } else {
                StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = ctx.getStateMachine();
                Role role = AgentRegistrationStates.FIRST_LEVEL_APPROVAL.role();
                User user = new User();
                user.setRole(role);
                Optional<User> firstApprovalOpt = userRepository.findOne(Example.of(user));
                if (!firstApprovalOpt.isPresent()) {
                    return;
                }
                User firstApproval = firstApprovalOpt.get();
                machine.getExtendedState().getVariables().put(Constants.LEVEL_1_APPROVER_ID, firstApproval.getId());

                registerAgent.setStatus(RegisterAgentStatus.PENDING_LEVEL_1_APPROVAL);
                registerAgent.setAssignee(firstApproval.getId());
                registerAgentRepository.save(registerAgent);
            }
        };
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> assignSecondLevelApproval() {
        return ctx -> {
            StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = ctx.getStateMachine();
            Role role = AgentRegistrationStates.SECOND_LEVEL_APPROVAL.role();
            User user = new User();
            user.setRole(role);
            Optional<User> secondApprovalOpt = userRepository.findOne(Example.of(user));
            if (!secondApprovalOpt.isPresent()) {
                return;
            }
            User secondApproval = secondApprovalOpt.get();
            machine.getExtendedState().getVariables().put(Constants.LEVEL_2_APPROVER_ID, secondApproval.getId());

            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(machine.getId());
            if (!registerAgentOptional.isPresent()) {
                return;
            }
            RegisterAgent registerAgent = registerAgentOptional.get();
            registerAgent.setStatus(RegisterAgentStatus.PENDING_LEVEL_2_APPROVAL);
            registerAgent.setAssignee(secondApproval.getId());
            registerAgentRepository.save(registerAgent);
        };
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> assignToUser() {
        return ctx -> {
            StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = ctx.getStateMachine();
            String userId = (String) machine.getExtendedState().getVariables().get(Constants.USER_ID);

            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(machine.getId());
            if (!registerAgentOptional.isPresent()) {
                return;
            }
            RegisterAgent registerAgent = registerAgentOptional.get();
            registerAgent.setAssignee(userId);
            registerAgentRepository.save(registerAgent);
        };
    }

    @Bean
    public Action<AgentRegistrationStates, AgentRegistrationEvents> assignToApprover() {
        return ctx -> {
            StateMachine<AgentRegistrationStates, AgentRegistrationEvents> machine = ctx.getStateMachine();
            String approver = (String) machine.getExtendedState().getVariables().get(Constants.LEVEL_2_APPROVER_ID);
            if (approver == null) {
                approver = (String) machine.getExtendedState().getVariables().get(Constants.LEVEL_1_APPROVER_ID);
            }

            Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(machine.getId());
            if (!registerAgentOptional.isPresent()) {
                return;
            }
            RegisterAgent registerAgent = registerAgentOptional.get();
            registerAgent.setAssignee(approver);
            registerAgentRepository.save(registerAgent);
        };
    }


    @Override
    public void configure(
            StateMachineTransitionConfigurer<AgentRegistrationStates, AgentRegistrationEvents> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(AgentRegistrationStates.SUBMITTED)
                .target(AgentRegistrationStates.FIRST_LEVEL_APPROVAL)
                .event(AgentRegistrationEvents.REGISTER)
                .action(activateUser())
                .action(assignFirstLevelApproval())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.FIRST_LEVEL_APPROVAL)
                .target(AgentRegistrationStates.SECOND_LEVEL_APPROVAL)
                .event(AgentRegistrationEvents.APPROVED)
                .action(assignSecondLevelApproval())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.FIRST_LEVEL_APPROVAL)
                .target(AgentRegistrationStates.PENDING)
                .event(AgentRegistrationEvents.INFO_NEEDED)
                .action(assignToUser())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.FIRST_LEVEL_APPROVAL)
                .target(AgentRegistrationStates.REJECTED)
                .event(AgentRegistrationEvents.REJECTED)
                .action(executeRejected())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.SECOND_LEVEL_APPROVAL)
                .target(AgentRegistrationStates.APPROVED)
                .event(AgentRegistrationEvents.APPROVED)
                .action(executeApproved())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.SECOND_LEVEL_APPROVAL)
                .target(AgentRegistrationStates.PENDING)
                .event(AgentRegistrationEvents.INFO_NEEDED)
                .action(assignToUser())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.SECOND_LEVEL_APPROVAL)
                .target(AgentRegistrationStates.REJECTED)
                .event(AgentRegistrationEvents.REJECTED)
                .action(executeRejected())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.PENDING)
                .target(AgentRegistrationStates.FIRST_LEVEL_APPROVAL)
                .event(AgentRegistrationEvents.INFO_PROVIDED)
                .action(assignToApprover())
                .and()
                .withExternal()
                .source(AgentRegistrationStates.PENDING)
                .target(AgentRegistrationStates.SECOND_LEVEL_APPROVAL)
                .event(AgentRegistrationEvents.INFO_PROVIDED)
                .action(assignToApprover());
    }


    public Agent createAgent(String van, RegisterAgent registerAgent, User user) {
        Agent agent = new Agent();
        agent.setId(CommonUtils.generateUUID());
        agent.setAgentType(registerAgent.getAgentType());
        agent.setAgencyId(registerAgent.getAgencyId());
        agent.setSubAgentType(registerAgent.getSubAgentType());
        agent.setAreaType(registerAgent.getAreaType());
        agent.setAgencyType(registerAgent.getAgencyType());
        agent.setVan(van);
        agent.setAgencyName(registerAgent.getAgencyName());
        agent.setDiscoms(registerAgent.getDiscoms());
        agent.setDivisions(registerAgent.getDivisions());
        agent.setPanNumber(registerAgent.getPanNumber());
        agent.setGstin(registerAgent.getGstin());
        agent.setRegistrationNumber(registerAgent.getRegistrationNumber());
        agent.setTinNumber(registerAgent.getTinNumber());
        agent.setDocuments(registerAgent.getDocuments());
        agent.setStatus(AgentStatus.ACTIVE);
        agent.setUser(user);
        agent.setAccountNumber(registerAgent.getAccountNumber());
        agent.setIfsc(registerAgent.getIfsc());
        agent.setEmpId(registerAgent.getEmpId());
        agent.setUniqueId(registerAgent.getUniqueId());
        agent.setDistrict(user.getAddress().getDistrict());
        if (registerAgent.getSubAgentType() == SubAgentType.METER_READER_AGENT) {
            agent.setSyncStatus(SyncStatus.SYNC_PENDING);
        }
        return agentRepository.save(agent);
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

    private String getVan(String id) {
        Optional<Agent> optAgent = agentRepository.findById(id);
        if (optAgent.isPresent()) {
            return optAgent.get().getVan();
        }
        logger.error("No agent found with id " + id);
        return null;
    }
}