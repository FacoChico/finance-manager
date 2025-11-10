package com.mephi.skillfactory.oop.finance.manager.service;

import com.mephi.skillfactory.oop.finance.manager.domain.Operation;
import com.mephi.skillfactory.oop.finance.manager.domain.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.mephi.skillfactory.oop.finance.manager.domain.enumeration.OperationType.EXPENSE;
import static java.lang.Math.abs;

@Service
public class AlertService {
    @Value("${limit-threshold}")
    private double limitThreshold;

    public void checkAlerts(User user, Operation recentOp, double spentByCategory, double totalIncome, double totalExpense) {
        if (EXPENSE.equals(recentOp.getType())) {
            final var category = recentOp.getCategory();
            final var budget = user.getWallet().getBudgets().get(category);

            if (budget != null) {
                final var remaining = budget.getLimit() - spentByCategory;
                if (remaining < 0) {
                    System.out.printf("Бюджет по категории '%s' превышен на %.2f%n", category, abs(remaining));
                } else {
                    if (remaining <= budget.getLimit() * limitThreshold) {
                        System.out.printf("Бюджет по категории '%s' близок к лимиту: остаток = %.2f%n", category, remaining);
                    }
                }
            }
        }

        if (totalExpense > totalIncome) {
            final var diff = abs(totalExpense - totalIncome);
            System.out.printf("Общие расходы (%.2f) превышают доходы!(%.2f)%nВы в минусе на (%.2f)!%n", totalExpense, totalIncome, diff);
        }
    }
}
