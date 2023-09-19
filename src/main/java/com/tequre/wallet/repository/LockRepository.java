package com.tequre.wallet.repository;

import com.tequre.wallet.data.Lock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LockRepository extends MongoRepository<Lock, String> {

}
