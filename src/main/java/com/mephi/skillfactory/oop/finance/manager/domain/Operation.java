package com.mephi.skillfactory.oop.finance.manager.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Operation {
    private UUID id;
    private OperationType type;
    private double amount;
    private String category;
    private String description;
    private Instant timestamp;
    private String fromUser;
    private String toUser;

    public Operation(OperationType type, double amount, String category, String description, String fromUser, String toUser) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.timestamp = Instant.now();
        this.fromUser = fromUser;
        this.toUser = toUser;
    }

    @JsonCreator
    public Operation(@JsonProperty("id") UUID id, @JsonProperty("type") OperationType type, @JsonProperty("amount") double amount,
                     @JsonProperty("category") String category, @JsonProperty("description") String description,
                     @JsonProperty("fromUser") String fromUser, @JsonProperty("toUser") String toUser) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.timestamp = Instant.now();
        this.fromUser = fromUser;
        this.toUser = toUser;
    }
}
