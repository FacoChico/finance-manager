package com.mephi.skillfactory.oop.finance.manager.service.auth;

import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.domain.Wallet;
import com.mephi.skillfactory.oop.finance.manager.repository.FileBasedCredentialsRepository;
import com.mephi.skillfactory.oop.finance.manager.repository.FileBasedWalletRepository;
import com.mephi.skillfactory.oop.finance.manager.service.auth.exception.IllegalCredentialsException;
import com.mephi.skillfactory.oop.finance.manager.service.exception.UserNotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(classes = AuthService.class)
class AuthServiceUnitTest {
    @MockitoBean
    private FileBasedWalletRepository walletRepository;
    @MockitoBean
    private FileBasedCredentialsRepository credentialsRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        // clearing data
        final var usersFiled = AuthService.class.getDeclaredField("users");
        usersFiled.setAccessible(true);
        usersFiled.set(authService, new HashMap<String, User>());

        final var credentialsFiled = AuthService.class.getDeclaredField("credentials");
        credentialsFiled.setAccessible(true);
        credentialsFiled.set(authService, new HashMap<String, String>());

        // mocking required beans
        doNothing()
            .when(credentialsRepository).saveCredentials(any());
        doNothing()
            .when(walletRepository).saveWallet(any());
        doReturn(new Wallet())
            .when(walletRepository).loadWallet(any());
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void shouldRegisterUser() {
        // given
        final var login = "login";
        final var password = "password";

        // when
        authService.register(login, password);

        // then
        final var expectedPasswordHash = DigestUtils.sha256Hex(password);

        // users field
        final var usersFiled = AuthService.class.getDeclaredField("users");
        usersFiled.setAccessible(true);

        final var usersMap = (Map<String, User>) usersFiled.get(authService);
        assertEquals(1, usersMap.size());
        final var savedUser = usersMap.get(login);
        assertEquals(login, savedUser.getLogin());
        assertEquals(expectedPasswordHash, savedUser.getPasswordHash());

        // credentials field
        final var credentialsFiled = AuthService.class.getDeclaredField("credentials");
        credentialsFiled.setAccessible(true);

        final var credentialsMap = (Map<String, String>) credentialsFiled.get(authService);
        assertEquals(1, credentialsMap.size());
        assertEquals(expectedPasswordHash, credentialsMap.get(login));
    }

    @ParameterizedTest
    @MethodSource("provideBadLoginOrPasswordArgs")
    @SneakyThrows
    void shouldThrowExceptionOnRegisterIfLoginIsNotPresent(String login) {
        final var password = "password";

        final var exception = assertThrows(IllegalCredentialsException.class, () -> authService.register(login, password));

        assertEquals("Логин или пароль не представлены", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideBadLoginOrPasswordArgs")
    @SneakyThrows
    void shouldThrowExceptionOnRegisterIfPasswordIsNotPresent(String password) {
        final var login = "login";

        final var exception = assertThrows(IllegalCredentialsException.class, () -> authService.register(login, password));

        assertEquals("Логин или пароль не представлены", exception.getMessage());
    }

    @Test
    @SneakyThrows
    void shouldThrowExceptionOnRegisterIfLoginIsAlreadyRegistered() {
        // given
        final var login = "login";
        final var password = "password";
        authService.register(login, password);

        // when
        final var exception = assertThrows(IllegalCredentialsException.class, () -> authService.register(login, password));

        // then
        assertEquals("Пользователь с логином %s уже зарегистрирован".formatted(login), exception.getMessage());
    }

    @Test
    @SneakyThrows
    void shouldLoginUser() {
        // given
        final var login = "login";
        final var password = "password";
        authService.register(login, password);

        // when
        final var user = authService.login(login, password);

        // then
        assertNotNull(user);
        assertEquals(login, user.getLogin());
        assertEquals(DigestUtils.sha256Hex(password), user.getPasswordHash());
    }

    @ParameterizedTest
    @MethodSource("provideBadLoginOrPasswordArgs")
    @SneakyThrows
    void shouldThrowExceptionOnLoginIfLoginIsNotPresent(String login) {
        final var password = "password";

        final var exception = assertThrows(IllegalCredentialsException.class, () -> authService.login(login, password));

        assertEquals("Логин или пароль не представлены", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideBadLoginOrPasswordArgs")
    @SneakyThrows
    void shouldThrowExceptionOnLoginIfPasswordIsNotPresent(String password) {
        final var login = "login";

        final var exception = assertThrows(IllegalCredentialsException.class, () -> authService.login(login, password));

        assertEquals("Логин или пароль не представлены", exception.getMessage());
    }

    @Test
    @SneakyThrows
    void shouldThrowExceptionOnLoginIfUserNotFound() {
        // given
        final var registeredLogin = "registeredLogin";
        final var password = "password";
        authService.register(registeredLogin, password);

        // when
        final var someLogin = "someLogin";
        final var exception = assertThrows(UserNotFoundException.class, () -> authService.login(someLogin, "somePassword"));

        // then
        assertEquals("Пользователь с логином %s не найден".formatted(someLogin), exception.getMessage());
    }

    @Test
    @SneakyThrows
    void shouldThrowExceptionOnLoginIfPasswordInIncorrect() {
        // given
        final var registeredLogin = "registeredLogin";
        final var password = "registeredPassword";
        authService.register(registeredLogin, password);

        // when
        final var exception = assertThrows(IllegalCredentialsException.class, () -> authService.login(registeredLogin, "somePassword"));

        // then
        assertEquals("Неправильный пароль", exception.getMessage());
    }

    private static Stream<Arguments> provideBadLoginOrPasswordArgs() {
        return Stream.of(
            arguments((Object) null), arguments(""), arguments("        ") // only whitespaces
        );
    }
}
