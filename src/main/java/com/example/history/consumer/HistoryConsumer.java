package com.example.history.consumer;

import com.example.account.repository.AccountRepository;
import com.example.history.entity.History;
import com.example.history.repository.HistoryRepository;
import com.example.history.request.HistoryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.example.util.Utils.parseData;

@Component
public class HistoryConsumer {

    private final HistoryRepository historyRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public HistoryConsumer(final HistoryRepository historyRepository,
                           final AccountRepository accountRepository) {
        this.historyRepository = historyRepository;
        this.accountRepository = accountRepository;
    }

    @KafkaListener(topics = "test-topic")
    void listener(final String data) throws JsonProcessingException {
        final String newData = parseData(data);
        final ObjectMapper mapper = new ObjectMapper();
        final HistoryRequest historyRequest = mapper.readValue(newData, HistoryRequest.class);

        final History history = this.getHistory(historyRequest);
        this.historyRepository.save(history);
    }

    private History getHistory(final HistoryRequest historyRequest) {
        final History history = new History();
        history.setAccount(this.accountRepository.findById(historyRequest.getAccountId()).get());
        history.setMessage(historyRequest.getMessage());
        history.setOperation(historyRequest.getOperation());
        history.setCurrentBalance(historyRequest.getCurrentBalance());
        return history;
    }
}
