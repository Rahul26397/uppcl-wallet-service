package com.tequre.wallet.service.producer;

import com.tequre.wallet.consumer.payment.PaymentNonRapdrpStream;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class PaymentNonRapdrpStreamService {

    private final Logger logger = LoggerFactory.getLogger(PaymentNonRapdrpStreamService.class);

    @Autowired
    private PaymentNonRapdrpStream paymentStream;

    @Autowired
    private EventRepository eventRepository;

    public Boolean produceEvent(Event event) {
        Event persistedEvent = eventRepository.save(event);
        logger.info("Event Published " + persistedEvent);
        MessageChannel messageChannel = paymentStream.producer();
        return messageChannel.send(MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build());
    }

    public Boolean replayEvent(Event event) {
        logger.info("Event Published " + event);
        MessageChannel messageChannel = paymentStream.producer();
        return messageChannel.send(MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build());
    }

}
