package com.tequre.wallet.service;

import java.util.Date;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tequre.wallet.data.Agent;

@Service
public class StorageService {
    @Autowired
    private MongoClient mongoClient;
    
    public void insertOne(Document data) {
    	MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        
        agentCollection.insertOne(data);
        System.out.println("Document inserted successfully");
    }
    
}
