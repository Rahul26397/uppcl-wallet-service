package com.tequre.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.response.AcceptedResponse;
import com.tequre.wallet.service.producer.AgentRegistrationStreamService;
import com.tequre.wallet.service.producer.PaymentNonRapdrpStreamService;
import com.tequre.wallet.service.producer.PaymentRapdrpStreamService;
import com.tequre.wallet.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1")
public class EventController {

    @Autowired
    private AgentRegistrationStreamService agentRegistrationStreamService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PaymentNonRapdrpStreamService paymentNonRapdrpStreamService;

    @Autowired
    private PaymentRapdrpStreamService paymentRapdrpStreamService;

    @Autowired
    private Gson gson;

    @RequestMapping(value = "/event/publish/payment/rapdrp", method = RequestMethod.POST)
    public ResponseEntity<?> publishRapdrpPayment(@RequestBody Event event) {
        event.setId(CommonUtils.generateUUID());
        event.setDate(new Date());
        paymentRapdrpStreamService.produceEvent(event);
        AcceptedResponse response = CommonUtils.eventResponse(event.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @RequestMapping(value = "/event/publish/payment/non-rapdrp", method = RequestMethod.POST)
    public ResponseEntity<?> publishNonRapdrpPayment(@RequestBody Event event) {
        event.setId(CommonUtils.generateUUID());
        event.setDate(new Date());
        paymentNonRapdrpStreamService.produceEvent(event);
        AcceptedResponse response = CommonUtils.eventResponse(event.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @RequestMapping(value = "/event/publish/agentRegistration", method = RequestMethod.POST)
    public ResponseEntity<?> publishAgentRegistration(@RequestBody Event event) {
        event.setId(CommonUtils.generateUUID());
        event.setDate(new Date());
        agentRegistrationStreamService.produceEvent(event);
        AcceptedResponse response = CommonUtils.eventResponse(event.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @RequestMapping(value = "/event/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getEvent(@PathVariable String id) {
        Optional<Event> eventOptional = eventRepository.findById(id);
        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            return ResponseEntity.ok(event);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/events", method = RequestMethod.GET)
    public ResponseEntity<?> getEvents(@RequestParam(value = "type", required = false) String type,
                                       @RequestParam(value = "status", required = false) EventStatus status,
                                       @RequestParam(value = "startTime", required = false) Long startTime,
                                       @RequestParam(value = "endTime", required = false) Long endTime,
                                       @RequestParam(value = "pageSize", required = false) String pageSize,
                                       @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (type != null) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("createdAt");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        query.with(pageableRequest);
        List<Event> agents = mongoTemplate.find(query, Event.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (agents != null) {
            element = gson.toJsonTree(agents, new TypeToken<List<Event>>() {
            }.getType());
            JsonArray array = element.getAsJsonArray();
            if (p.getSize() - array.size() == 0) {
                object.addProperty("nextPageToken", CommonUtils.nextPageToken(gson, p));
            }
            object.addProperty("recordCount", array.size());
            object.add("result", array);
        }
        JsonNode jsonNode = CommonUtils.getMapper().readTree(object.toString());
        return ResponseEntity.status(HttpStatus.OK).body(jsonNode);
    }

}
