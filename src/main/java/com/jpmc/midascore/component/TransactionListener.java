package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;

    @Autowired
    public TransactionListener(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
        this.restTemplate = new RestTemplate();
    }

    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);

        UserRecord sender = databaseConduit.findById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findById(transaction.getRecipientId());

        if (sender != null && recipient != null && sender.getBalance() >= transaction.getAmount()) {
            float incentiveAmount = 0.0f;
            try {
                Incentive incentive = restTemplate.postForObject("http://localhost:8080/incentive", transaction, Incentive.class);
                if (incentive != null) {
                    incentiveAmount = incentive.getAmount();
                }
            } catch (Exception e) {
                logger.error("Failed to query Incentive API, applying 0 incentive", e);
            }

            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);
            databaseConduit.save(sender);
            databaseConduit.save(recipient);

            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
            databaseConduit.save(transactionRecord);

            logger.info("Transaction processed successfully: sender {} new balance {}, recipient {} new balance {} (incentive: {})", 
                sender.getName(), sender.getBalance(), recipient.getName(), recipient.getBalance(), incentiveAmount);
        } else {
            logger.warn("Transaction failed validation: {}", transaction);
        }
    }
}
