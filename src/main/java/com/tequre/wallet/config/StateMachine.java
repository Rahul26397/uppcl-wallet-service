package com.tequre.wallet.config;

import com.tequre.wallet.enums.AgentRegistrationEvents;
import com.tequre.wallet.enums.AgentRegistrationStates;
import com.tequre.wallet.repository.AgentStateMachineRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.data.mongodb.MongoDbPersistingStateMachineInterceptor;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

@Configuration
public class StateMachine {

    @Bean
    public StateMachineRuntimePersister<AgentRegistrationStates,
            AgentRegistrationEvents, String> stateMachineRuntimePersister(
            AgentStateMachineRepository agentStateMachineRepository) {
        return new MongoDbPersistingStateMachineInterceptor<>(agentStateMachineRepository);
    }

    @Bean
    public StateMachineService<AgentRegistrationStates,
            AgentRegistrationEvents> stateMachineService(
            StateMachineFactory<AgentRegistrationStates,
                    AgentRegistrationEvents> stateMachineFactory,
            StateMachineRuntimePersister<AgentRegistrationStates,
                    AgentRegistrationEvents, String> stateMachineRuntimePersister) {
        return new DefaultStateMachineService<>(stateMachineFactory, stateMachineRuntimePersister);
    }

}
