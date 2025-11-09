package com.mephi.skillfactory.oop.finance.manager.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Budget {
    private String category;
    private double limit;

    @JsonCreator
    public Budget(@JsonProperty("category") String category, @JsonProperty("limit") double limit) {
        this.category = category;
        this.limit = limit;
    }
}
