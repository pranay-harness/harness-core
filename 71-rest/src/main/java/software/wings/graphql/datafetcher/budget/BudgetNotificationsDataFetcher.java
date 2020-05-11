package software.wings.graphql.datafetcher.budget;

import com.google.inject.Inject;

import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.Budget;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetNotifications;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetNotificationsData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class BudgetNotificationsDataFetcher
    extends AbstractObjectDataFetcher<QLBudgetNotificationsData, QLBudgetQueryParameters> {
  @Inject BudgetService budgetService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLBudgetNotificationsData fetch(QLBudgetQueryParameters parameters, String accountId) {
    List<Budget> budgets = budgetService.list(accountId);
    int count = 0;
    for (Budget budget : budgets) {
      AlertThreshold[] alertThresholds = budget.getAlertThresholds();
      if (alertThresholds == null) {
        continue;
      }
      for (int i = 0; i < alertThresholds.length; i++) {
        if (alertThresholds[i].getAlertsSent() > 0) {
          count++;
        }
      }
    }
    return QLBudgetNotificationsData.builder().data(QLBudgetNotifications.builder().count(count).build()).build();
  }
}
