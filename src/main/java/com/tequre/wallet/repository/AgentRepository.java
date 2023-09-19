package com.tequre.wallet.repository;

import com.tequre.wallet.data.Agent;
import com.tequre.wallet.request.RegisterAgentRequest;

import java.util.Optional;

import javax.validation.Valid;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends MongoRepository<Agent, String> {

	// Optional<Agent> saveAll(@Valid RegisterAgentRequest registerAgentRequest);

	Optional<Agent> save(@Valid RegisterAgentRequest registerAgentRequest);
	
	@Query(value = "{'van': ?0}", fields = "{'_id': 1}")
    String findAgentIdByVan(String van);

	
}
