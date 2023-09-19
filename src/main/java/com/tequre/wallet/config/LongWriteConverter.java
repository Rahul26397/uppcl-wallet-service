package com.tequre.wallet.config;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Date;

public class LongWriteConverter implements Converter<Long, Date> {

    @Override
    public Date convert(Long time) {
        return Date.from(Instant.ofEpochMilli(time));
    }
}
