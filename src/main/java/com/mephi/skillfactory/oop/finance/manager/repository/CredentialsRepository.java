package com.mephi.skillfactory.oop.finance.manager.repository;

import java.util.Map;

public interface CredentialsRepository {

    Map<String, String> loadCredentials();

    void saveCredentials(Map<String, String> credentials);
}
