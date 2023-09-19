package com.tequre.wallet.repository;

import com.tequre.wallet.data.RegisterAgent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterAgentRepository extends MongoRepository<RegisterAgent, String> {

}
