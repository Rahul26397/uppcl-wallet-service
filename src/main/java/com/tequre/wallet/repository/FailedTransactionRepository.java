package com.tequre.wallet.repository;

import com.tequre.wallet.data.FailedTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedTransactionRepository extends MongoRepository<FailedTransaction, String> {

}
