package io.harness.ccm.budget;

import com.google.inject.Inject;

import io.harness.ccm.budget.entities.Budget;

import java.util.List;

public class BudgetServiceImpl implements BudgetService {
  @Inject private BudgetDao budgetDao;

  @Override
  public String create(Budget budget) {
    return budgetDao.save(budget);
  }

  @Override
  public void update(String budgetId, Budget budget) {
    budgetDao.update(budgetId, budget);
  }

  public Budget get(String budgetId) {
    return budgetDao.get(budgetId);
  }

  @Override
  public List<Budget> list(String accountId) {
    return budgetDao.list(accountId, 0, 0);
  }

  @Override
  public List<Budget> list(String accountId, Integer count, Integer startIndex) {
    return budgetDao.list(accountId, count, startIndex);
  }

  @Override
  public boolean delete(String budgetId) {
    return budgetDao.delete(budgetId);
  }
}
