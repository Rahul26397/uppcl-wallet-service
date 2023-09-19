package com.tequre.wallet.service;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import org.bson.BsonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

@Service
public class DistrictService {
    @Autowired
    private MongoClient mongoClient;
    public AggregateIterable getDistricts(){
    /*
    Arrays.asList(new Document("$unwind",
    new Document("path", "$districtName")
            .append("preserveNullAndEmptyArrays", true)),
    new Document("$sort",
    new Document("discomName", 1L)),
    new Document("$group",
    new Document("_id",
    new BsonNull())
            .append("districts",
    new Document("$addToSet", "$districtName"))))
     */
        MongoDatabase database = mongoClient.getDatabase("wallet");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        result = database
                .getCollection("uppcl_hierarchy").aggregate(Arrays.asList(new Document("$group",
                        new Document("_id", "null")
                                .append("districts",
                                        new Document("$addToSet", "$districtName")))));
        return result;
    }
}
