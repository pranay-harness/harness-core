package software.wings.graphql.datafetcher.budget;

import com.google.inject.Inject;

import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.Budget;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class BudgetListDataFetcher extends AbstractArrayDataFetcher<QLBudgetTableData, QLBudgetQueryParameters> {
  @Inject BudgetService budgetService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<QLBudgetTableData> fetch(QLBudgetQueryParameters parameters, String accountId) {
    List<Budget> budgets = budgetService.list(accountId);
    List<QLBudgetTableData> budgetTableDataList = new ArrayList<>();
    budgets.forEach(budget -> budgetTableDataList.add(budgetService.getBudgetDetails(budget)));
    budgetTableDataList.sort(Comparator.comparing(QLBudgetTableData::getLastUpdatedAt).reversed());
    return budgetTableDataList;
  }

  @Override
  protected QLBudgetTableData unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
