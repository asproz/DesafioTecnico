package com.example.history.producer;

import com.example.history.request.HistoryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class HistoryProducer {

    @Value("${topic.name.producer}")
    private String topicName;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public HistoryProducer(final KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(final HistoryRequest history) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonInString = mapper.writeValueAsString(history);

            this.kafkaTemplate.send(this.topicName, jsonInString);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Ocorreu um erro", e.getCause());
        }
    }
}
