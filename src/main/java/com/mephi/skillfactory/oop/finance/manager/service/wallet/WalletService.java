package com.mephi.skillfactory.oop.finance.manager.service.wallet;

import com.mephi.skillfactory.oop.finance.manager.domain.Budget;
import com.mephi.skillfactory.oop.finance.manager.domain.Operation;
import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType;
import com.mephi.skillfactory.oop.finance.manager.repository.FileBasedWalletRepository;
import com.mephi.skillfactory.oop.finance.manager.repository.exception.FileContentTypeMismatchException;
import com.mephi.skillfactory.oop.finance.manager.service.AlertService;
import com.mephi.skillfactory.oop.finance.manager.service.auth.AuthService;
import com.mephi.skillfactory.oop.finance.manager.service.exception.UserNotFoundException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.AmountException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.BudgetException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.CategoryNotFoundException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.WalletImportSourceException;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.EXPENSE;
import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.INCOME;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
@RequiredArgsConstructor
public class WalletService {
    private static final String WITHOUT_CATEGORY = "Без категории";
    private final AuthService authService;
    private final FileBasedWalletRepository walletRepository;
    private final AlertService alertService;

    public void addIncome(User user, double amount, String category, String description) throws AmountException {
        validateAmount(amount);
        final var operation = new Operation(INCOME, amount, category, description, null, user.getLogin());
        user.getWallet().addOperation(operation);

        alertService.checkAlerts(user, operation, sumByOperationTypeAndCategory(user.getWallet().getOperations(), EXPENSE, null)
            .getOrDefault(category, 0.0), totalIncome(user), totalExpense(user));
    }

    public void addExpense(User user, double amount, String category, String description) throws AmountException {
        validateAmount(amount);
        if (isBlank(category)) {
            category = WITHOUT_CATEGORY;
        }
        final var operation = new Operation(EXPENSE, amount, category, description, user.getLogin(), null);
        user.getWallet().addOperation(operation);

        alertService.checkAlerts(user, operation, sumByOperationTypeAndCategory(user.getWallet().getOperations(), EXPENSE, null)
            .getOrDefault(category, 0.0), totalIncome(user), totalExpense(user));
    }

    public void renameCategory(User user, String oldCategoryName, String newCategoryName) throws CategoryNotFoundException {
        final var operations = user.getWallet().getOperations();
        final var budgets = user.getWallet().getBudgets();

        final var operationsWithCategoryToRename = operations.stream()
            .filter(operation -> operation.getCategory().equals(oldCategoryName))
            .toList();
        final var categoryBudget = Optional.ofNullable(budgets.remove(oldCategoryName));

        if (operationsWithCategoryToRename.isEmpty() && categoryBudget.isEmpty()) {
            throw new CategoryNotFoundException("Категория %s не найдена".formatted(oldCategoryName));
        }

        operationsWithCategoryToRename
            .forEach(operation -> operation.setCategory(newCategoryName));
        categoryBudget
            .ifPresent(budget -> {
                budget.setCategory(newCategoryName);
                budgets.put(newCategoryName, budget);
            });
    }

    public void transfer(String fromLogin, String toLogin, double amount, String category,
                         String description) throws AmountException, UserNotFoundException {

        validateAmount(amount);

        final var fromUser = authService.findUser(fromLogin);
        final var toUser = authService.findUser(toLogin);
        validateUser(fromUser, "Отправитель не найден");
        validateUser(toUser, "Получатель не найден");

        final var expense = new Operation(EXPENSE, amount, category, description, fromLogin, toLogin);
        fromUser.getWallet().addOperation(expense);
        final var income = new Operation(INCOME, amount, category, description, fromLogin, toLogin);
        toUser.getWallet().addOperation(income);

        saveUserWallet(fromUser);
        saveUserWallet(toUser);

        alertService.checkAlerts(fromUser, expense, sumByOperationTypeAndCategory(fromUser.getWallet().getOperations(), EXPENSE, null)
            .getOrDefault(category, 0.0), totalIncome(fromUser), totalExpense(fromUser));
        alertService.checkAlerts(toUser, income, sumByOperationTypeAndCategory(toUser.getWallet().getOperations(), EXPENSE, null)
            .getOrDefault(category, 0.0), totalIncome(toUser), totalExpense(toUser));
    }

    public void setBudget(User user, String category, double limit) throws BudgetException {
        if (isBlank(category)) {
            throw new BudgetException("Категория не представлена");
        }

        final var budget = new Budget(category, limit);
        user.getWallet().getBudgets().put(category, budget);
    }

    public void changeBudgetLimit(User user, String category, double limit) throws BudgetException {
        if (isBlank(category)) {
            throw new BudgetException("Категория не представлена");
        }

        final var budget = Optional.ofNullable(user.getWallet().getBudgets().get(category))
            .orElseThrow(() -> new BudgetException("Бюджет для категории %s не найден".formatted(category)));

        budget.setLimit(limit);
    }


    public void deleteBudget(User user, String category) throws BudgetException {
        if (isBlank(category)) {
            throw new BudgetException("Категория не представлена");
        }

        Optional.ofNullable(user.getWallet().getBudgets().remove(category))
            .orElseThrow(() -> new BudgetException("Бюджет для категории %s не найден".formatted(category)));
    }

    public double totalIncome(User user) {
        return user.getWallet().getOperations().stream()
            .filter(operation -> INCOME.equals(operation.getType()))
            .mapToDouble(Operation::getAmount)
            .sum();
    }

    public double totalExpense(User user) {
        return user.getWallet().getOperations().stream()
            .filter(operation -> EXPENSE.equals(operation.getType()))
            .mapToDouble(Operation::getAmount)
            .sum();
    }

    public Map<String, Double> sumByOperationTypeAndCategory(List<Operation> operations, OperationType operationType, @Nullable String category) {
        return operations.stream()
            .filter(operation -> {
                final var matchesOpType = operationType.equals(operation.getType());
                return category == null
                    ? matchesOpType
                    : matchesOpType && category.equals(operation.getCategory());
            })
            .collect(Collectors.groupingBy(
                operation -> operation.getCategory() == null
                    ? WITHOUT_CATEGORY
                    : operation.getCategory(), Collectors.summingDouble(Operation::getAmount)
            ));
    }

    private void validateAmount(double amount) throws AmountException {
        if (amount <= 0) {
            throw new AmountException("Сумма должна быть больше 0");
        }
    }

    public void saveUserWallet(User user) {
        walletRepository.saveWallet(user);
    }

    public void importWalletForUser(String source, User user) throws WalletImportSourceException, FileContentTypeMismatchException {
        if (isBlank(source)) {
            throw new WalletImportSourceException("Передан пустой путь");
        }

        if (!source.endsWith(".json")) {
            throw new WalletImportSourceException(
                "Файл по пути %s имеет недопустимое расширение. Допустимые расширения: %s".formatted(source, ".json"));
        }

        final var src = Paths.get(source).toAbsolutePath();
        if (!Files.exists(src) || !Files.isRegularFile(src)) {
            throw new WalletImportSourceException("Файл не найден или не является обычным файлом: " + src);
        }

        walletRepository.importWallet(src, user);
    }

    private void validateUser(User user, String errorMessage) throws UserNotFoundException {
        if (user == null) {
            throw new UserNotFoundException(errorMessage);
        }
    }
}
