package com.mephi.skillfactory.oop.finance.manager.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FileBasedCredentialsRepository implements CredentialsRepository {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final File credentialsFile;

    public FileBasedCredentialsRepository() {
        final var dataDir = new File("data");
        if (!dataDir.exists()) {
            final var ignored = dataDir.mkdirs();
        }

        credentialsFile = new File(dataDir, "credentials.json");
    }

    @Override
    public Map<String, String> loadCredentials() {
        if (!credentialsFile.exists()) {
            return new ConcurrentHashMap<>();
        }

        try {
            // use TypeReference to read Map<String,String>
            final Map<String, String> credentials = mapper.readValue(credentialsFile, new TypeReference<>() {
            });
            return new ConcurrentHashMap<>(credentials);
        } catch (IOException e) {
            System.err.println("Ошибка загрузки credentials: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

    @Override
    public void saveCredentials(Map<String, String> credentials) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(credentialsFile, credentials);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения данных: " + e.getMessage());
        }
    }
}
