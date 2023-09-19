package com.tequre.wallet.controller;

import com.mongodb.client.AggregateIterable;
import com.tequre.wallet.service.DistrictService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/list")
public class DistrictController {
    @Autowired
    private DistrictService districtService;
    @GetMapping("/hierarchy/district")
    public ResponseEntity<?> getHierarchyDistrict(){
        // return (ResponseEntity<?>) ResponseEntity.ok(districtService.getDistricts());
        AggregateIterable<Document> result = districtService.getDistricts();
        List<Object> documents = new ArrayList<>();
        for (Document dbObject : result) {
            dbObject.remove("_id");
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }
}
