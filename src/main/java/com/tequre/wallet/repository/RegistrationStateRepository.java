package com.tequre.wallet.repository;

import com.tequre.wallet.data.RegistrationState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationStateRepository extends MongoRepository<RegistrationState, String> {

}
