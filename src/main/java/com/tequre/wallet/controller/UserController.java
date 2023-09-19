package com.tequre.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.data.User;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.enums.Role;
import com.tequre.wallet.enums.UserStatus;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.repository.UserRepository;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.request.UserCreateRequest;
import com.tequre.wallet.request.UserUpdateRequest;
import com.tequre.wallet.response.AcceptedResponse;
import com.tequre.wallet.service.UserService;
import com.tequre.wallet.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private Gson gson;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getUser(@PathVariable String id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            userRepository.deleteById(id);
            userService.deleteUser(userOptional.get().getUserName());
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getUsers(@RequestParam(value = "status", required = false) UserStatus status,
                                      @RequestParam(value = "username", required = false) String userName,
                                      @RequestParam(value = "email", required = false) String email,
                                      @RequestParam(value = "startTime", required = false) Long startTime,
                                      @RequestParam(value = "endTime", required = false) Long endTime,
                                      @RequestParam(value = "pageSize", required = false) String pageSize,
                                      @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (userName != null) {
            query.addCriteria(Criteria.where("userName").is(userName));
        }
        if (email != null) {
            query.addCriteria(Criteria.where("email").is(email));
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
        List<User> users = mongoTemplate.find(query, User.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (users != null) {
            element = gson.toJsonTree(users, new TypeToken<List<User>>() {
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

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (userUpdateRequest.getAddress() != null) {
                user.setAddress(userUpdateRequest.getAddress());
            }
            if (userUpdateRequest.getFirstName() != null) {
                user.setFirstName(userUpdateRequest.getFirstName());
            }
            if (userUpdateRequest.getLastName() != null) {
                user.setLastName(userUpdateRequest.getLastName());
            }
            if (userUpdateRequest.getMobile() != null) {
                user.setMobile(userUpdateRequest.getMobile());
            }
            if (userUpdateRequest.getResidenceAddress() != null) {
                user.setResidenceAddress(userUpdateRequest.getResidenceAddress());
            }
            if (userUpdateRequest.getImageUrl() != null) {
                user.setImageUrl(userUpdateRequest.getImageUrl());
            }
            return ResponseEntity.ok(userRepository.save(user));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> create(@Valid @RequestBody UserCreateRequest userCreateRequest) {
        if (userCreateRequest.getRole() == Role.AGENCY_REPORT_VIEWER) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Report Viewer user creation is not allowed.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Event event = CommonUtils.createEvent(userCreateRequest);
        AcceptedResponse response = CommonUtils.eventResponse(event.getId());
        try {
            createUser(userCreateRequest);
            event.setStatus(EventStatus.SUCCESS);
            logger.info("User creation successful: {}", userCreateRequest);
        } catch (Throwable e) {
            event.setStatus(EventStatus.FAILED);
            event.setReason(e.getMessage());
            logger.error("An exception occurred while processing user create request.", e);
        } finally {
            eventRepository.save(event);
            logger.info("Event Processed Successful " + event);
        }
        return ResponseEntity.accepted().body(response);
    }

    private User createUser(UserCreateRequest userCreateRequest) {
        User user = new User();
        user.setId(CommonUtils.generateUUID());
        user.setEmail(userCreateRequest.getEmail());
        user.setFirstName(userCreateRequest.getFirstName());
        user.setLastName(userCreateRequest.getLastName());
        user.setStatus(UserStatus.PENDING);
        user.setMobile(userCreateRequest.getMobile());
        user.setUserName(userCreateRequest.getUserName());
        user.setPassword(CommonUtils.generatePassword());
        user.setAddress(userCreateRequest.getAddress());
        user.setResidenceAddress(userCreateRequest.getResidenceAddress());
        logger.info("ROLE: CREATE REQUEST: "+userCreateRequest.getRole());
        user.setRole(userCreateRequest.getRole());
        user.setImageUrl(userCreateRequest.getImageUrl());
        User created = userRepository.save(user);
        try {
            userService.createUser(created.getUserName(), created.getPassword(), created.getEmail(), created.getRole());
        } catch (Throwable th) {
            userRepository.deleteById(user.getId());
            logger.error("Unable to create auth user " + created.getUserName(), th);
            throw th;
        }
        return created;
    }
}
