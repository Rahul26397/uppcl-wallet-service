package com.tequre.wallet.utils;

import static com.tequre.wallet.utils.Constants.DEFAULT_PAGE_SIZE;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.response.AcceptedResponse;

@Component
public class CommonUtils {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String generateTransactionUUID() {
        return UUID.randomUUID().toString();
    }

    public static Event createEvent(Object payload) {
        Event event = new Event();
        event.setId(CommonUtils.generateUUID());
        event.setStatus(EventStatus.IN_QUEUE);
        event.setDate(new Date());
        event.setType(payload.getClass().getSimpleName());
        event.setPayload(payload);
        return event;
    }

    public static AcceptedResponse eventResponse(String id) {
        AcceptedResponse response = new AcceptedResponse();
        response.setLocation("/v1/event/" + id);
        response.setRetryAfter(60);
        return response;
    }

    public static String generatePassword() {
        String upperCaseLetters = RandomStringUtils.random(2, 65, 90, true, true);
        String lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true);
        String numbers = RandomStringUtils.randomNumeric(2);
        String specialChar = RandomStringUtils.random(2, 33, 47, false, false);
        String totalChars = RandomStringUtils.randomAlphanumeric(2);
        String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
                .concat(numbers)
                .concat(specialChar)
                .concat(totalChars);
        List<Character> pwdChars = combinedChars.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(pwdChars);
        String password = pwdChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return password;
    }

    public static String generateVan() {
        long number = (long) Math.floor(Math.random() * 9_999_999_999L);// + 1_000_000_000L;
        return String.format("%010d", number);
    }

    public static String generateTransactionId() {
        return "EW" + Instant.now().toEpochMilli();
    }

    public static String generateWalletTransferTransactionId() {
        return "EWT" + Instant.now().toEpochMilli();
    }

    public static Page getPage(Gson gson, String pageSize, String nextPageToken) {
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : Integer.parseInt(pageSize);
        Page p;
        if (nextPageToken != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(nextPageToken);
            p = gson.fromJson(new String(decodedBytes), Page.class);
            if (p.getSize() != size) {
                throw new IllegalStateException("Invalid Page token");
            }
        } else {
            p = new Page();
            p.setPage(0);
            p.setSize(size);
        }
        return p;
    }

    public static String nextPageToken(Gson gson, Page page) {
        page.setPage(page.getPage() + 1);
        return Base64.getEncoder().encodeToString(gson.toJson(page).getBytes());
    }

    public static ObjectMapper getMapper() {
        return objectMapper;
    }

    public static String getCurrentDate() {
        Date date = Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime();
        return DATE_FORMAT.format(date);
    }

    public static LocalDate getLocalDate(String date) {
        return LocalDate.parse(date, formatter);
    }

    public static Long toEpochMilli(String date) {
        ZoneId timeZone = ZoneId.of("Asia/Kolkata");
        LocalDate localDate = getLocalDate(date);
        return localDate.atStartOfDay(timeZone).toInstant().toEpochMilli();
    }

    public static List<Long> range(String date, int amount) {
        ZoneId timeZone = ZoneId.of("Asia/Kolkata");
        LocalDate startDate = getLocalDate(date);
        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIDNIGHT);
        long start = startDateTime.atZone(timeZone).toInstant().toEpochMilli();
        LocalDateTime endDateTime = startDateTime.plus(amount, ChronoUnit.HOURS);
        long end = endDateTime.atZone(timeZone).toInstant().toEpochMilli();
        if (start < end) {
            return Arrays.asList(start, end);
        } else {
            return Arrays.asList(end, start);
        }
    }
}
