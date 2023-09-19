package com.tequre.wallet.consumer.payment;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface PaymentNonRapdrpStream {

    String INBOUND = "payment-non-rapdrp-consumer";
    String OUTBOUND = "payment-non-rapdrp-producer";

    @Input(INBOUND)
    SubscribableChannel consumer();

    @Output(OUTBOUND)
    MessageChannel producer();
}
