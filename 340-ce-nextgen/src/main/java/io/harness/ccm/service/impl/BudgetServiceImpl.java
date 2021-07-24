package io.harness.ccm.service.impl;

import io.harness.ccm.budget.AlertThreshold;
import io.harness.ccm.budget.BudgetScope;
import io.harness.ccm.budget.dao.BudgetDao;
import io.harness.ccm.budget.utils.BudgetUtils;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.service.intf.BudgetService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
public class BudgetServiceImpl implements BudgetService {
  @Inject private BudgetDao budgetDao;
  @Inject private CEViewService ceViewService;

  @Override
  public String create(Budget budget) {
    BudgetUtils.validateBudget(budget, budgetDao.list(budget.getAccountId(), budget.getName()));
    removeEmailDuplicates(budget);
    validatePerspective(budget);
    // Todo : Update costs here
    return budgetDao.save(budget);
  }

  @Override
  public String clone(String budgetId, String cloneBudgetName, String accountId) {
    Budget budget = budgetDao.get(budgetId, accountId);
    BudgetUtils.validateBudget(budget, budgetDao.list(budget.getAccountId(), budget.getName()));
    BudgetUtils.validateCloneBudgetName(cloneBudgetName);
    Budget cloneBudget = Budget.builder()
                             .accountId(budget.getAccountId())
                             .name(cloneBudgetName)
                             .scope(budget.getScope())
                             .type(budget.getType())
                             .budgetAmount(budget.getBudgetAmount())
                             .actualCost(budget.getActualCost())
                             .forecastCost(budget.getForecastCost())
                             .lastMonthCost(budget.getLastMonthCost())
                             .alertThresholds(budget.getAlertThresholds())
                             .userGroupIds(budget.getUserGroupIds())
                             .emailAddresses(budget.getEmailAddresses())
                             .notifyOnSlack(budget.isNotifyOnSlack())
                             .build();
    return create(cloneBudget);
  }

  @Override
  public Budget get(String budgetId, String accountId) {
    return budgetDao.get(budgetId, accountId);
  }

  @Override
  public void update(String budgetId, Budget budget) {
    if (budget.getAccountId() == null) {
      Budget existingBudget = budgetDao.get(budgetId);
      budget.setAccountId(existingBudget.getAccountId());
    }
    if (budget.getUuid() == null) {
      budget.setUuid(budgetId);
    }
    BudgetUtils.validateBudget(budget, budgetDao.list(budget.getAccountId(), budget.getName()));
    removeEmailDuplicates(budget);
    validatePerspective(budget);
    // Todo : Update costs here
    log.info("Budget {}", budget);
    budgetDao.update(budgetId, budget);
  }

  @Override
  public List<Budget> list(String accountId) {
    return budgetDao.list(accountId, Integer.MAX_VALUE - 1, 0);
  }

  @Override
  public List<Budget> list(String accountId, String perspectiveId) {
    List<Budget> budgets = budgetDao.list(accountId, Integer.MAX_VALUE - 1, 0);
    return budgets.stream()
        .filter(budget -> isBudgetBasedOnGivenPerspective(budget, perspectiveId))
        .collect(Collectors.toList());
  }

  @Override
  public boolean delete(String budgetId, String accountId) {
    return budgetDao.delete(budgetId, accountId);
  }

  private void validatePerspective(Budget budget) {
    BudgetScope scope = budget.getScope();
    String[] entityIds = BudgetUtils.getAppliesToIds(scope);
    log.debug("entityIds is {}", entityIds);
    if (ceViewService.get(entityIds[0]) == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_ENTITY_ID_EXCEPTION);
    }
  }

  public boolean isBudgetBasedOnGivenPerspective(Budget budget, String perspectiveId) {
    return budget.getScope().getEntityIds().get(0).equals(perspectiveId);
  }

  private void removeEmailDuplicates(Budget budget) {
    String[] emailAddresses = ArrayUtils.nullToEmpty(budget.getEmailAddresses());
    String[] uniqueEmailAddresses = new HashSet<>(Arrays.asList(emailAddresses)).toArray(new String[0]);
    budget.setEmailAddresses(uniqueEmailAddresses);
    // In NG we have per alertThreshold separate email addresses
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    if (alertThresholds != null && alertThresholds.length > 0) {
      for (AlertThreshold alertThreshold : alertThresholds) {
        emailAddresses = ArrayUtils.nullToEmpty(alertThreshold.getEmailAddresses());
        uniqueEmailAddresses = new HashSet<>(Arrays.asList(emailAddresses)).toArray(new String[0]);
        alertThreshold.setEmailAddresses(uniqueEmailAddresses);
      }
      budget.setAlertThresholds(alertThresholds);
    }
  }
}
