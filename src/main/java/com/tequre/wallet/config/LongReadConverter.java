package com.tequre.wallet.config;

import org.springframework.core.convert.converter.Converter;

import java.util.Date;

public class LongReadConverter implements Converter<Date, Long> {

    @Override
    public Long convert(Date date) {
        return date.toInstant().toEpochMilli();
    }
}
