package com.mephi.skillfactory.oop.finance.manager.service.auth;

import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.repository.PersistenceService;
import com.mephi.skillfactory.oop.finance.manager.service.auth.exception.IllegalCredentialsException;
import com.mephi.skillfactory.oop.finance.manager.service.exception.UserNotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
public class AuthService {
    private final Map<String, User> users;
    private final Map<String, String> credentials;
    private final PersistenceService persistenceService;

    public AuthService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        this.users = new ConcurrentHashMap<>();
        this.credentials = new ConcurrentHashMap<>();

        final var loadedCredentials = persistenceService.loadCredentials();
        if (loadedCredentials != null) {
            credentials.putAll(loadedCredentials);

            loadedCredentials.forEach((login, passwordHash) -> {
                final var user = new User(login, passwordHash);
                final var wallet = persistenceService.loadWallet(login);
                user.setWallet(wallet);

                users.put(login, user);
            });
        }
    }

    public void register(String login, String password) throws IllegalCredentialsException {
        if (isBlank(login) || isBlank(password)) {
            throw new IllegalCredentialsException("Логин или пароль не представлены");
        }
        if (credentials.containsKey(login)) {
            throw new IllegalCredentialsException("Пользователь с логином %s уже зарегистрирован".formatted(login));
        }

        final var hash = DigestUtils.sha256Hex(password);
        credentials.put(login, hash);
        // сохраняем credentials на диск
        persistenceService.saveCredentials(credentials);

        // создаём User в памяти и сохраняем пустой кошелёк (чтобы файл
        // data/<login>.json появился)
        final var user = new User(login, hash);
        users.put(login, user);
        persistenceService.saveWallet(user);
    }

    public User login(String login, String password) throws IllegalCredentialsException, UserNotFoundException {
        if (isBlank(login) || isBlank(password)) {
            throw new IllegalCredentialsException("Логин или пароль не представлены");
        }

        final var storedHash = credentials.get(login);
        if (storedHash == null) {
            // нет учётной записи — просим зарегистрироваться
            throw new UserNotFoundException("Пользователь с логином %s не найден".formatted(login));
        }

        final var hash = DigestUtils.sha256Hex(password);
        if (!storedHash.equals(hash)) {
            // неверный пароль
            throw new IllegalCredentialsException("Неправильный пароль");
        }

        // проверка пройдена — создаём или достаём объект User из памяти и подгружаем
        // кошелёк
        final User user;
        if (!users.containsKey(login)) {
            user = new User(login, storedHash);
            user.setWallet(persistenceService.loadWallet(login));
            users.put(login, user);
        } else {
            user = users.get(login);
            // Обновим кошелёк с диска (чтобы учесть изменения, если они были)
            user.setWallet(persistenceService.loadWallet(login));
        }
        return user;
    }

    public User findUser(String login) {
        return users.get(login);
    }

    public Map<String, User> getAllUsers() {
        return users;
    }
}
