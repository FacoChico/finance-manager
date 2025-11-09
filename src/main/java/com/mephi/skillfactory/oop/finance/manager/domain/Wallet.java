package com.mephi.skillfactory.oop.finance.manager.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.EXPENSE;
import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.INCOME;

@Getter
@Setter
public class Wallet {
    private double balance;
    private List<Operation> operations;
    private Map<String, Budget> budgets;

    public Wallet() {
        this.balance = 0.0;
        this.operations = new ArrayList<>();
        this.budgets = new HashMap<>();
    }

    @JsonCreator
    public Wallet(@JsonProperty("balance") double balance, @JsonProperty("operations") List<Operation> operations,
                  @JsonProperty("budgets") Map<String, Budget> budgets) {
        this.balance = balance;
        this.operations = operations;
        this.budgets = budgets;
    }

    public void addOperation(Operation op) {
        operations.add(op);

        if (INCOME.equals(op.getType())) {
            balance += op.getAmount();
        } else if (EXPENSE.equals(op.getType())) {
            balance -= op.getAmount();
        }
    }
}
