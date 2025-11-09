package com.mephi.skillfactory.oop.finance.manager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.domain.Wallet;
import com.mephi.skillfactory.oop.finance.manager.repository.exception.FileContentTypeMismatchException;

import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Repository
public class FileBasedWalletRepository implements WalletRepository {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final File dataDir = new File("data");

    public FileBasedWalletRepository() {
        if (!dataDir.exists()) {
            final var ignored = dataDir.mkdirs();
        }
    }

    @Override
    public void saveWallet(User user) {
        final var walletInfoFile = new File(dataDir, user.getLogin() + ".json");

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(walletInfoFile, user.getWallet());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения кошелька для " + user.getLogin() + ": " + e.getMessage());
        }
    }

    @Override
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

    @Override
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
}
