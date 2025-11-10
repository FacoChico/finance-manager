package com.mephi.skillfactory.oop.finance.manager.cli;

import com.mephi.skillfactory.oop.finance.manager.domain.User;
import com.mephi.skillfactory.oop.finance.manager.repository.exception.FileContentTypeMismatchException;
import com.mephi.skillfactory.oop.finance.manager.service.auth.AuthService;
import com.mephi.skillfactory.oop.finance.manager.service.auth.exception.IllegalCredentialsException;
import com.mephi.skillfactory.oop.finance.manager.service.exception.UserNotFoundException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.WalletService;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.AmountException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.BudgetException;
import com.mephi.skillfactory.oop.finance.manager.service.wallet.exception.WalletImportSourceException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

import lombok.RequiredArgsConstructor;

import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.EXPENSE;
import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.INCOME;
import static java.util.Objects.requireNonNullElse;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {
    private final AuthService authService;
    private final WalletService walletService;

    @Override
    public void run(String... args) {
        System.out.println("Finance Manager CLI started.\nВведите 'help' для просмотра доступных команд.");
        final var scanner = new Scanner(System.in);
        User currentUser = null;

        Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> {
                System.out.println("Завершение работы");
                authService.getAllUsers().values().forEach(walletService::saveUserWallet);
            }));

        whileOuter:
        while (true) {
            printUsername(currentUser);

            final String line;
            try {
                if (!scanner.hasNextLine()) {
                    System.out.println();
                    break;
                }
                line = scanner.nextLine();
            } catch (NoSuchElementException | IllegalStateException e) {
                break;
            }

            if (line == null) {
                break;
            }
            final var parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isBlank()) {
                continue;
            }

            final var cmd = parts[0].toLowerCase();
            try {
                switch (cmd) {
                    case "help":
                        printHelp();
                        break;
                    case "register":
                        if (parts.length != 3) {
                            System.out.println("Использование: register <login> <password>");
                            break;
                        }

                        try {
                            final var login = parts[1];
                            authService.register(login, parts[2]);
                            System.out.printf("Пользователь %s зарегистрирован%n", login);
                        } catch (IllegalCredentialsException e) {
                            System.out.printf("Не удалось зарегистрировать пользователя: %s%n", e.getMessage());
                        }
                        break;
                    case "login":
                        if (parts.length != 3) {
                            System.out.println("Использование: login <login> <password>");
                            break;
                        }
                        try {
                            currentUser = authService.login(parts[1], parts[2]);
                            System.out.printf("Авторизация с логином %s успешно пройдена%n", currentUser.getLogin());
                        } catch (IllegalCredentialsException | UserNotFoundException e) {
                            System.out.printf("Не удалось провести аутентификацию: %s%n".formatted(e.getMessage()));
                        }
                        break;
                    case "logout":
                        if (isUserLoggedIn(currentUser)) {
                            walletService.saveUserWallet(currentUser);
                            final var login = currentUser.getLogin();
                            currentUser = null;
                            System.out.printf("Осуществлен выход из аккаунта %s%n", login);
                        }
                        break;
                    case "add-income":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для добавления доходов необходимо авторизоваться");
                            break;
                        }
                        if (parts.length < 3 || parts.length > 4) {
                            System.out.println("Использование: add-income <amount> <category> [description]");
                            break;
                        }
                        final var incomeAmount = Double.parseDouble(parts[1]);
                        final var incomeCategory = parts[2];
                        final var incomeDescription = parts.length > 3
                            ? join(parts, 3)
                            : "";

                        try {
                            walletService.addIncome(currentUser, incomeAmount, incomeCategory, incomeDescription);
                            System.out.println("Доход добавлен");
                        } catch (AmountException e) {
                            System.out.printf("Не удалось добавить доход: %s%n", e.getMessage());
                        }
                        break;
                    case "add-expense":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для добавления расходов необходимо авторизоваться");
                            break;
                        }
                        if (parts.length == 3 || parts.length == 4) {
                            final var expenseAmount = Double.parseDouble(parts[1]);
                            final var expenseCategory = parts[2];
                            final var expenseDescription = parts.length > 3
                                ? join(parts, 3)
                                : "";

                            try {
                                walletService.addExpense(currentUser, expenseAmount, expenseCategory, expenseDescription);
                                System.out.println("Расходы добавлены");
                            } catch (AmountException e) {
                                System.out.printf("Не удалось добавить расходы: %s%n", e.getMessage());
                            }
                        } else {
                            System.out.println("Использование: add-expense <amount> <category> [description]");
                        }
                        break;
                    case "set-budget":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для установки бюджета необходимо авторизоваться");
                            break;
                        }
                        if (parts.length != 3) {
                            System.out.println("Использование: set-budget <category> <amount>");
                            break;
                        }

                        final var budgetCategory = parts[1];
                        final var budgetAmount = Double.parseDouble(parts[2]);

                        try {
                            walletService.setBudget(currentUser, budgetCategory, budgetAmount);
                            System.out.println("Бюджет установлен");
                        } catch (BudgetException e) {
                            System.out.printf("Бюджет не установлен: %s%n", e.getMessage());
                        }
                        break;
                    case "transfer":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для осуществления переводов необходимо авторизоваться");
                            break;
                        }
                        if (parts.length < 3 || parts.length > 5) {
                            System.out.println("Использование: transfer <toLogin> <amount> [category] [description]");
                            break;
                        }

                        final var transferTo = parts[1];
                        final var transferAmount = Double.parseDouble(parts[2]);
                        final var transferCategory = parts.length > 3
                            ? parts[3]
                            : "Перевод";
                        final var transferDescription = parts.length > 4
                            ? join(parts, 4)
                            : "";

                        try {
                            walletService.transfer(currentUser.getLogin(), transferTo, transferAmount, transferCategory, transferDescription);
                            System.out.println("Перевод осуществлен");
                        } catch (AmountException | UserNotFoundException e) {
                            System.out.printf("Не удалось осуществить перевод: %s%n", e.getMessage());
                        }
                        break;
                    case "summary":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для получения сводной статистики необходимо авторизоваться");
                            break;
                        }

                        printSummary(currentUser);
                        break;
                    case "summary-by-categories":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для получения статистики по категориям необходимо авторизоваться");
                            break;
                        }
                        if (parts.length < 2) {
                            System.out.println("Использование: summary-by-categories <category1 ... categoryN>");
                            break;
                        }

                        printCategoriesSummary(parts, currentUser);
                        break;
                    case "export":
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для экспорта данных необходимо авторизоваться");
                            break;
                        }

                        walletService.saveUserWallet(currentUser);
                        System.out.println("Кошелек сохранен по пути 'data/" + currentUser.getLogin() + ".json'");
                        break;
                    case "import":
                        if (parts.length < 2) {
                            System.out.println("Использование: import <path/to/wallet-file.json>");
                            break;
                        }
                        if (isUserNotLoggedIn(currentUser)) {
                            System.out.println("Для импорта данных необходимо авторизоваться");
                            break;
                        }

                        final var importPath = join(parts, 1);
                        try {
                            walletService.importWalletForUser(importPath, currentUser);
                        } catch (WalletImportSourceException | FileContentTypeMismatchException e) {
                            System.out.printf("Ошибка во время импорта кошелька: %s%n", e.getMessage());
                        }
                        break;
                    case "exit":
                        authService.getAllUsers().values().forEach(walletService::saveUserWallet);
                        System.out.println("Выход из приложения осуществлен");
                        break whileOuter;
                    default:
                        System.out.println("Неизвестная команда. Введите 'help' для просмотра доступных команд.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат числа: " + e.getMessage());
            } catch (Throwable e) {
                System.out.println("Ошибка во время работы приложения: " + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }

    private void printUsername(User currentUser) {
        final var username = currentUser == null
            ? "[guest]> "
            : "[" + currentUser.getLogin() + "]> ";
        System.out.print(username);
    }

    private void printHelp() {
        System.out
            .println("""
                Доступные команды:
                  register <login> <password>                          - регистрация
                  login <login> <password>                             - авторизация
                  logout                                               - выход из аккаунта
                  add-income <amount> <category> [description]         - добавление дохода
                  add-expense <amount> <category> [description]        - добавление расхода
                  set-budget <category> <amount>                       - установить бюджет для категории
                  transfer <toLogin> <amount> [category] [description] - перевод
                  summary                                              - сводная статистика по кошельку
                  summary-by-categories <category1 ... categoryN>      - сводная статистика по категории/категориям
                  export                                               - сохранить кошелек в файл 'data/<login>.json'
                  import <path/to/wallet-file.json>                    - импорт кошелька из json-файла в 'data/<login>.json' (кошелек будет присвоен текущему пользователю)
                  exit                                                 - выход
                """);
    }

    private boolean isUserLoggedIn(User user) {
        return user != null;
    }

    private boolean isUserNotLoggedIn(User user) {
        return !isUserLoggedIn(user);
    }

    private String join(String[] parts, int from) {
        final var sb = new StringBuilder();
        for (int i = from; i < parts.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private void printSummary(User user) {
        final var incomes = walletService.totalIncome(user);
        final var expenses = walletService.totalExpense(user);
        // TODO: есть поле balance, и это должно работать не так, переработать
        System.out.printf("В общем на счету: %.2f%n", incomes - expenses);

        System.out.printf("%nДоходы за все время: %.2f%n", incomes);
        final var incomeByCategoryMap = walletService.sumByOperationTypeAndCategory(user.getWallet().getOperations(), INCOME, null);
        if (!incomeByCategoryMap.isEmpty()) {
            System.out.println("Доходы по категориям:");
            incomeByCategoryMap.forEach((k, v) -> System.out.printf("  %s: %.2f%n", k, v));
        }

        System.out.printf("%nРасходы за все время: %.2f%n", expenses);
        final var expenseByCategoryMap = walletService.sumByOperationTypeAndCategory(user.getWallet().getOperations(), EXPENSE, null);
        if (!expenseByCategoryMap.isEmpty()) {
            System.out.println("Расходы по категориям:");
            expenseByCategoryMap.forEach((k, v) -> System.out.printf("  %s: %.2f%n", k, v));
        }

        final var budgets = user.getWallet().getBudgets();
        if (!budgets.isEmpty()) {
            System.out.printf("%nБюджет по категориям:%n");
            budgets.forEach((k, v) -> {
                final var spent = walletService.sumByOperationTypeAndCategory(user.getWallet().getOperations(), EXPENSE, null)
                    .getOrDefault(k, 0.0);
                final var remaining = v.getLimit() - spent;
                System.out.printf("  %s: %.2f, Оставшийся бюджет: %.2f%n", k, v.getLimit(), remaining);
            });
        }
    }

    private void printCategoriesSummary(String[] parts, User user) {
        final var operations = user.getWallet().getOperations();
        final var budgets = user.getWallet().getBudgets();

        for (var i = 1; i < parts.length; i++) {
            final var category = parts[i];
            final var categoryIncomes = walletService.sumByOperationTypeAndCategory(operations, INCOME, category);
            final var categoryExpenses = walletService.sumByOperationTypeAndCategory(operations, EXPENSE, category);
            final var categoryBudgetOptional = Optional.ofNullable(budgets.get(category));

            if (categoryIncomes.isEmpty() && categoryExpenses.isEmpty() && categoryBudgetOptional.isEmpty()) {
                System.out.printf("Данных по категории %s не найдено%n", category);
            } else {
                System.out.printf("Данные по категории %s:%n", category);
                final var incomes = requireNonNullElse(categoryIncomes.get(category), 0.0);
                System.out.printf("  Доходы: %.2f%n", incomes);
                final var expenses = requireNonNullElse(categoryExpenses.get(category), 0.0);
                System.out.printf("  Расходы: %.2f%n", expenses);

                categoryBudgetOptional.ifPresent(budget -> {
                    final var spent = walletService.sumByOperationTypeAndCategory(user.getWallet().getOperations(), EXPENSE, category)
                        .getOrDefault(category, 0.0);
                    final var remaining = budget.getLimit() - spent;
                    System.out.printf("  Бюджет: %.2f, Оставшийся бюджет: %.2f%n", budget.getLimit(), remaining);
                });
            }
        }
    }
}
