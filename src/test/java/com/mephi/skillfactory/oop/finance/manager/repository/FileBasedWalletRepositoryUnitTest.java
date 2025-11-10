package com.mephi.skillfactory.oop.finance.manager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mephi.skillfactory.oop.finance.manager.domain.Operation;
import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.domain.Wallet;
import com.mephi.skillfactory.oop.finance.manager.repository.exception.FileContentTypeMismatchException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.SneakyThrows;

import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.INCOME;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBasedWalletRepositoryUnitTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @SneakyThrows
    void shouldSaveWalletToFile(@TempDir Path tempDir) {
        // given
        final var repository = new FileBasedWalletRepository(tempDir.toString());
        final var login = "login";
        final var user = new User(login, "passwordHash");
        final var opId = UUID.randomUUID();
        final var op = new Operation(opId, INCOME, 250, "someCategory", "description", login, null);
        final var wallet = new Wallet(250, List.of(op), emptyMap());
        user.setWallet(wallet);

        // when
        repository.saveWallet(user);

        // then
        final var jsonFile = tempDir.resolve(login + ".json");
        final var fileContents = Files.readString(jsonFile);

        final var readWallet = MAPPER.readValue(fileContents, Wallet.class);

        assertEquals(250.0, readWallet.getBalance());
        assertEquals(1, readWallet.getOperations().size());
        final var readOp = readWallet.getOperations().getFirst();
        assertEquals(opId, readOp.getId());
        assertEquals(op.getTimestamp(), readOp.getTimestamp());
    }

    @Test
    @SneakyThrows
    void shouldReturnEmptyWalletIfFileIsEmptyOrNull(@TempDir Path tempDir) {
        // given
        final var repository = new FileBasedWalletRepository(tempDir.toString());

        // when
        final var loadedWallet = repository.loadWallet("login");

        // then
        assertNotNull(loadedWallet);
        assertEquals(0, loadedWallet.getBalance());
        assertEquals(0, loadedWallet.getOperations().size());
        assertEquals(0, loadedWallet.getBudgets().size());
    }

    @Test
    @SneakyThrows
    void shouldLoadWalletFromFile(@TempDir Path tempDir) {
        // given
        final var repository = new FileBasedWalletRepository(tempDir.toString());
        final var login = "login";
        final var user = new User(login, "passwordHash");
        final var opId = UUID.randomUUID();
        final var op = new Operation(opId, INCOME, 250, "someCategory", "description", login, null);
        final var wallet = new Wallet(250, List.of(op), emptyMap());
        user.setWallet(wallet);

        repository.saveWallet(user);

        // when
        final var loadedWallet = repository.loadWallet(login);

        // then
        assertEquals(250.0, loadedWallet.getBalance());
        assertEquals(1, loadedWallet.getOperations().size());
        final var readOp = loadedWallet.getOperations().getFirst();
        assertEquals(opId, readOp.getId());
        assertEquals(op.getTimestamp(), readOp.getTimestamp());
    }

    @Test
    @SneakyThrows
    void shouldImportWallet(@TempDir Path tempDir) {
        // given
        final var repository = new FileBasedWalletRepository(tempDir.toString());
        final var login = "login";
        final var user = new User(login, "passwordHash");
        final var opId = UUID.randomUUID();
        final var op = new Operation(opId, INCOME, 250, "someCategory", "description", login, null);

        final var wallet = new Wallet(250, List.of(op), emptyMap());
        user.setWallet(wallet);

        final var src = tempDir.resolve("wallet-to-import.json");

        MAPPER.writerWithDefaultPrettyPrinter().writeValue(src.toFile(), wallet);

        // when
        repository.importWallet(src, user);

        // then
        final var jsonFile = tempDir.resolve(login + ".json");
        assertTrue(Files.exists(jsonFile));

        final var readWallet = MAPPER.readValue(jsonFile.toFile(), Wallet.class);
        assertNotNull(readWallet);
        assertEquals(wallet.getBalance(), readWallet.getBalance());
        assertEquals(1, readWallet.getOperations().size());

        final var readOp = readWallet.getOperations().getFirst();
        assertEquals(opId, readOp.getId());
        assertEquals(op.getTimestamp(), readOp.getTimestamp());

        assertNotNull(user.getWallet());
        assertEquals(wallet.getBalance(), user.getWallet().getBalance());
        assertEquals(opId, user.getWallet().getOperations().getFirst().getId());
    }

    @Test
    @SneakyThrows
    void shouldThrowExceptionIfImportWalletFileIsInvalidJson(@TempDir Path tempDir) {
        // given
        final var repository = new FileBasedWalletRepository(tempDir.toString());
        final var login = "login";
        final var user = new User(login, "passwordHash");

        final var bad = tempDir.resolve("bad.json");
        Files.writeString(bad, "{ not valid json }");

        // when
        assertThrows(FileContentTypeMismatchException.class, () -> repository.importWallet(bad, user));

        // then
        final var jsonFile = tempDir.resolve(login + ".json");
        assertFalse(Files.exists(jsonFile));

        final var loadedWallet = user.getWallet();
        assertNotNull(loadedWallet);
        assertEquals(0, loadedWallet.getBalance());
        assertEquals(0, loadedWallet.getOperations().size());
        assertEquals(0, loadedWallet.getBudgets().size());
    }

    @Test
    @SneakyThrows
    void shouldReplaceExistingFileWhileImporting(@TempDir Path tempDir) {
        // given
        final var repository = new FileBasedWalletRepository(tempDir.toString());
        final var login = "login";
        final var user = new User(login, "passwordHash");

        // create existing wallet json file (as old content)
        final var oldOp = new Operation(UUID.randomUUID(), INCOME, 10.0, "old", "old", login, null);
        oldOp.setTimestamp(Instant.parse("2024-01-01T00:00:00Z"));
        final var oldWallet = new Wallet(10.0, List.of(oldOp), emptyMap());
        final var existingJson = tempDir.resolve(login + ".json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(existingJson.toFile(), oldWallet);

        // new wallet to import
        final var newOp = new Operation(UUID.randomUUID(), INCOME, 500.0, "new", "new", login, null);
        final var now = Instant.now();
        newOp.setTimestamp(now);
        final var newWallet = new Wallet(500.0, List.of(newOp), emptyMap());
        final var newJson = tempDir.resolve("new.json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(newJson.toFile(), newWallet);

        // when
        repository.importWallet(newJson, user);

        // then
        // destination replaced with new content
        final var fromDisk = MAPPER.readValue(existingJson.toFile(), Wallet.class);
        assertEquals(500.0, fromDisk.getBalance());
        assertEquals(1, fromDisk.getOperations().size());
        assertEquals(newOp.getId(), fromDisk.getOperations().getFirst().getId());

        // user wallet updated
        assertNotNull(user.getWallet());
        assertEquals(500.0, user.getWallet().getBalance());
    }
}
