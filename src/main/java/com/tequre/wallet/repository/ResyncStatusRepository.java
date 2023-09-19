package com.tequre.wallet.repository;

import com.tequre.wallet.data.ResyncStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResyncStatusRepository extends MongoRepository<ResyncStatus, String> {

}
