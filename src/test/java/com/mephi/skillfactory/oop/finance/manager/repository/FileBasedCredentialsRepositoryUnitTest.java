package com.mephi.skillfactory.oop.finance.manager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBasedCredentialsRepositoryUnitTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @SneakyThrows
    void shouldSaveCredentialsToFile(@TempDir Path tempDir) {
        // given
        final var credentialsFileName = "credentials.json";
        final var repository = new FileBasedCredentialsRepository(tempDir.toString(), credentialsFileName);

        final var user1Name = "user1";
        final var user2Name = "user2";
        final var user1PasswordHash = "hash1";
        final var user2PasswordHash = "hash2";

        // when
        repository.saveCredentials(Map.of(user1Name, user1PasswordHash, user2Name, user2PasswordHash));

        // then
        final var jsonFile = tempDir.resolve(credentialsFileName);
        final var read = Files.readString(jsonFile);

        final var readCredentials = MAPPER.readValue(read, Map.class);

        assertTrue(readCredentials.containsKey(user1Name));
        assertEquals(user1PasswordHash, readCredentials.get(user1Name));

        assertTrue(readCredentials.containsKey(user2Name));
        assertEquals(user2PasswordHash, readCredentials.get(user2Name));

        final var loaded = repository.loadCredentials();
        assertNotNull(loaded);
        assertEquals(2, loaded.size());
        assertEquals(user1PasswordHash, loaded.get(user1Name));
        assertEquals(user2PasswordHash, loaded.get(user2Name));
    }

    @Test
    @SneakyThrows
    void shouldReturnEmptyWhenCredentialsFileIsEmpty(@TempDir Path tempDir) {
        // given
        final var credentialsFileName = "credentials.json";
        final var repository = new FileBasedCredentialsRepository(tempDir.toString(), credentialsFileName);

        // when
        final var loaded = repository.loadCredentials();

        // then
        assertNotNull(loaded);
        assertEquals(0, loaded.size());
    }

    @Test
    @SneakyThrows
    void shouldLoadCredentialsFromFile(@TempDir Path tempDir) {
        // given
        final var credentialsFileName = "credentials.json";
        final var repository = new FileBasedCredentialsRepository(tempDir.toString(), credentialsFileName);

        final var user1Name = "user1";
        final var user2Name = "user2";
        final var user1PasswordHash = "hash1";
        final var user2PasswordHash = "hash2";

        repository.saveCredentials(Map.of(
            user1Name, user1PasswordHash, user2Name, user2PasswordHash
        ));

        // when
        final var loaded = repository.loadCredentials();

        // then
        assertNotNull(loaded);
        assertEquals(2, loaded.size());
        assertEquals(user1PasswordHash, loaded.get(user1Name));
        assertEquals(user2PasswordHash, loaded.get(user2Name));
    }
}
