package com.tequre.wallet.controller;

import com.tequre.wallet.data.RegisterAgent;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.enums.AgentRegistrationEvents;
import com.tequre.wallet.enums.AgentRegistrationStates;
import com.tequre.wallet.enums.RegisterAgentStatus;
import com.tequre.wallet.repository.RegisterAgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/fsm")
public class StateMachineController {

    @Autowired
    private StateMachineService<AgentRegistrationStates, AgentRegistrationEvents> stateMachineService;

    @Autowired
    private RegisterAgentRepository registerAgentRepository;

    @RequestMapping(value = "/event/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> sendEvent(@PathVariable String id, @RequestBody Map<String, Object> data) {
        Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(id);
        if (!registerAgentOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        RegisterAgent registerAgent = registerAgentOptional.get();
        if (registerAgent.getStatus() != RegisterAgentStatus.FULFILLED
                && registerAgent.getStatus() != RegisterAgentStatus.REJECTED) {
            String eventStr = data.get("event").toString();
            AgentRegistrationEvents event = AgentRegistrationEvents.valueOf(eventStr);
            if (event != null) {
                StateMachine<AgentRegistrationStates, AgentRegistrationEvents> stateMachine = getStateMachine(id, true);
                stateMachine.sendEvent(event);
                return ResponseEntity.accepted().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("Agent registration request is already processed.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @RequestMapping(value = "/event/{id}/{event}", method = RequestMethod.GET)
    public ResponseEntity<?> sendEvent(@PathVariable String id, @PathVariable String event) {
        Optional<RegisterAgent> registerAgentOptional = registerAgentRepository.findById(id);
        if (!registerAgentOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        RegisterAgent registerAgent = registerAgentOptional.get();
        if (registerAgent.getStatus() != RegisterAgentStatus.FULFILLED
                && registerAgent.getStatus() != RegisterAgentStatus.REJECTED) {
            String eventStr = event;
            AgentRegistrationEvents e = AgentRegistrationEvents.valueOf(eventStr);
            if (event != null) {
                StateMachine<AgentRegistrationStates, AgentRegistrationEvents> stateMachine = getStateMachine(id, true);
                stateMachine.sendEvent(e);
                return ResponseEntity.accepted().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("Agent registration request is already processed.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private synchronized StateMachine<AgentRegistrationStates,
            AgentRegistrationEvents> getStateMachine(String id, boolean start) {
        StateMachine<AgentRegistrationStates, AgentRegistrationEvents> stateMachine = stateMachineService
                .acquireStateMachine(id, start);
        if (start) {
           /*  StateMachineListener stateMachineListener = new StateMachineListener(registrationStateRepository,
                    registerAgentRepository,
                    stateMachineService); */
            //stateMachine.addStateListener(stateMachineListener);
        }
        return stateMachine;
    }
}
