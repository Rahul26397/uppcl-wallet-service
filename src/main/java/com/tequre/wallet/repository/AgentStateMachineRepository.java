package com.tequre.wallet.repository;

import org.springframework.statemachine.data.mongodb.MongoDbStateMachineRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentStateMachineRepository extends MongoDbStateMachineRepository {

}
