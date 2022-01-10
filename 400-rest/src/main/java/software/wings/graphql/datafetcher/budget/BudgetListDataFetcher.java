/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.budget;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.commons.entities.billing.Budget;

import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BudgetListDataFetcher extends AbstractArrayDataFetcher<QLBudgetTableData, QLBudgetQueryParameters> {
  @Inject BudgetService budgetService;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<QLBudgetTableData> fetch(QLBudgetQueryParameters parameters, String accountId) {
    accountChecker.checkIsCeEnabled(accountId);
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
