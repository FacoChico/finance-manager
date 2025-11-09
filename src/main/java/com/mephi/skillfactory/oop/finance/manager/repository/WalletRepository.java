package com.mephi.skillfactory.oop.finance.manager.repository;

import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.domain.Wallet;
import com.mephi.skillfactory.oop.finance.manager.repository.exception.FileContentTypeMismatchException;

import java.nio.file.Path;

public interface WalletRepository {

    void saveWallet(User user);

    void importWallet(Path src, User user) throws FileContentTypeMismatchException;

    Wallet loadWallet(String login);
}
