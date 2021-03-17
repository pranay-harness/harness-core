package io.harness.migrations.all;

import static io.harness.ccm.budget.entities.BudgetScopeType.APPLICATION;
import static io.harness.ccm.budget.entities.BudgetScopeType.CLUSTER;
import static io.harness.ccm.budget.entities.BudgetScopeType.PERSPECTIVE;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.budget.BudgetUtils;
import io.harness.ccm.budget.dao.BudgetDao;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.BudgetScope;
import io.harness.ccm.budget.entities.ClusterBudgetScope;
import io.harness.ccm.budget.entities.EnvironmentType;
import io.harness.ccm.budget.entities.PerspectiveBudgetScope;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@TargetModule(Module._390_DB_MIGRATION)
public class BudgetsMigration implements Migration {
  private final WingsPersistence wingsPersistence;
  private final BudgetDao budgetDao;
  private final BudgetUtils budgetUtils;

  @Inject
  public BudgetsMigration(WingsPersistence wingsPersistence, BudgetDao budgetDao, BudgetUtils budgetUtils) {
    this.wingsPersistence = wingsPersistence;
    this.budgetDao = budgetDao;
    this.budgetUtils = budgetUtils;
  }

  @Override
  public void migrate() {
    try {
      log.info("Starting migration of all Budgets");

      List<Budget> budgets = wingsPersistence.createQuery(Budget.class, excludeValidate).asList();
      List<io.harness.ccm.budget.Budget> budgetsForMigration = new ArrayList<>();
      budgets.forEach(budget
          -> budgetsForMigration.add(io.harness.ccm.budget.Budget.builder()
                                         .accountId(budget.getAccountId())
                                         .actualCost(budget.getActualCost())
                                         .budgetAmount(budget.getBudgetAmount())
                                         .createdAt(budget.getCreatedAt())
                                         .emailAddresses(budget.getEmailAddresses())
                                         .forecastCost(budget.getForecastCost())
                                         .lastMonthCost(budget.getLastMonthCost())
                                         .lastUpdatedAt(budget.getLastUpdatedAt())
                                         .name(budget.getName())
                                         .scope(getBudgetScope(budget.getScope()))
                                         .notifyOnSlack(budget.isNotifyOnSlack())
                                         .userGroupIds(budget.getUserGroupIds())
                                         .uuid(budget.getUuid())
                                         .alertThresholds(budget.getAlertThresholds())
                                         .type(budget.getType())
                                         .build()));

      budgetsForMigration.forEach(budget -> budgetUtils.updateBudgetCosts(budget, null));
      budgetDao.save(budgetsForMigration);
    } catch (Exception e) {
      log.error("Failure occurred in BudgetsMigration", e);
    }
    log.info("BudgetsMigration has completed");
  }

  private io.harness.ccm.budget.BudgetScope getBudgetScope(BudgetScope scope) {
    switch (scope.getBudgetScopeType()) {
      case APPLICATION:
        ApplicationBudgetScope appScope = (ApplicationBudgetScope) scope;
        return io.harness.ccm.budget.ApplicationBudgetScope.builder()
            .applicationIds(appScope.getApplicationIds())
            .environmentType(getEnvType(appScope.getEnvironmentType()))
            .build();
      case CLUSTER:
        ClusterBudgetScope clusterScope = (ClusterBudgetScope) scope;
        return io.harness.ccm.budget.ClusterBudgetScope.builder().clusterIds(clusterScope.getClusterIds()).build();
      case PERSPECTIVE:
        PerspectiveBudgetScope perspectiveBudgetScope = (PerspectiveBudgetScope) scope;
        return io.harness.ccm.budget.PerspectiveBudgetScope.builder()
            .viewId(perspectiveBudgetScope.getViewId())
            .viewName(perspectiveBudgetScope.getViewName())
            .build();
      default:
        return null;
    }
  }

  private io.harness.ccm.budget.EnvironmentType getEnvType(EnvironmentType type) {
    switch (type) {
      case PROD:
        return io.harness.ccm.budget.EnvironmentType.PROD;
      case ALL:
        return io.harness.ccm.budget.EnvironmentType.ALL;
      case NON_PROD:
        return io.harness.ccm.budget.EnvironmentType.NON_PROD;
      default:
        return null;
    }
  }
}
