package com.mephi.skillfactory.oop.finance.manager.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.domain.Wallet;
import com.mephi.skillfactory.oop.finance.manager.repository.exception.FileContentTypeMismatchException;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: переделать в репозиторий
@Service
public class PersistenceService {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final File dataDir = new File("data");
    private final File credentialsFile;

    public PersistenceService() {
        if (!dataDir.exists()) {
            final var ignored = dataDir.mkdirs();
        }
        credentialsFile = new File(dataDir, "credentials.json");
    }

    public void saveWallet(User user) {
        final var walletInfoFile = new File(dataDir, user.getLogin() + ".json");

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(walletInfoFile, user.getWallet());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения кошелька для " + user.getLogin() + ": " + e.getMessage());
        }
    }

    public void importWallet(Path src, User user) throws FileContentTypeMismatchException {
        final Wallet importedWallet;
        try {
            importedWallet = mapper.readValue(src.toFile(), Wallet.class);
        } catch (IOException e) {
            throw new FileContentTypeMismatchException("Структура импортируемого файла не поддерживается");
        }
        final var importedWalletFile = new File(dataDir, user.getLogin() + ".json");

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(importedWalletFile, importedWallet);
        } catch (IOException e) {
            throw new RuntimeException("Неизвестная ошибка импорта кошелька из " + src + ": " + e.getMessage());
        }

        user.setWallet(importedWallet);
    }

    public Wallet loadWallet(String login) {
        final var walletInfoFile = new File(dataDir, login + ".json");
        if (!walletInfoFile.exists()) {
            return new Wallet();
        }

        try {
            return mapper.readValue(walletInfoFile, Wallet.class);
        } catch (IOException e) {
            System.err.println("Ошибка загрузки кошелька для " + login + ": " + e.getMessage());
            return new Wallet();
        }
    }

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

    public void saveCredentials(Map<String, String> credentials) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(credentialsFile, credentials);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения данных: " + e.getMessage());
        }
    }
}
