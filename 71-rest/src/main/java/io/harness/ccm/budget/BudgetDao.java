package io.harness.ccm.budget;

import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;

import com.google.inject.Inject;

import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetKeys;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

public class BudgetDao {
  @Inject private HPersistence persistence;

  public String save(Budget budget) {
    return persistence.save(budget);
  }

  public Budget get(String budgetId) {
    Query<Budget> query = persistence.createQuery(Budget.class).field(BudgetKeys.uuid).equal(budgetId);
    return query.get();
  }

  public List<Budget> list(String accountId, Integer count, Integer startIndex) {
    Query<Budget> query = persistence.createQuery(Budget.class).field(BudgetKeys.accountId).equal(accountId);
    return query.asList(new FindOptions().skip(startIndex).limit(count));
  }

  public void update(String budgetId, Budget budget) {
    Query query = persistence.createQuery(Budget.class).field(BudgetKeys.uuid).equal(budgetId);
    UpdateOperations<Budget> updateOperations = persistence.createUpdateOperations(Budget.class)
                                                    .set(BudgetKeys.name, budget.getName())
                                                    .set(BudgetKeys.scope, budget.getScope())
                                                    .set(BudgetKeys.type, budget.getType());

    if (SPECIFIED_AMOUNT.equals(budget.getType())) {
      updateOperations = updateOperations.set(BudgetKeys.budgetAmount, budget.getBudgetAmount());
    }
    if (null != budget.getAlertThresholds()) {
      updateOperations.set(BudgetKeys.alertThresholds, budget.getAlertThresholds());
    }
    if (null != budget.getUserGroupId()) {
      updateOperations.set(BudgetKeys.userGroupId, budget.getUserGroupId());
    }
    persistence.update(query, updateOperations);
  }

  public boolean delete(String budgetId) {
    return persistence.delete(Budget.class, budgetId);
  }
}
