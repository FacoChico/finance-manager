package com.mephi.skillfactory.oop.finance.manager.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private final String login;
    private final String passwordHash; // sha256
    private Wallet wallet;

    public User(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.wallet = new Wallet();
    }
}
