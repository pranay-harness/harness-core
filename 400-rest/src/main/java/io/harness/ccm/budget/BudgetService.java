/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.billing.Budget;

import software.wings.graphql.schema.type.aggregation.budget.QLBudgetDataList;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;

import java.util.List;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public interface BudgetService {
  String create(Budget budgetRecord);
  String clone(String budgetId, String budgetName, String accountId);

  Budget get(String budgetId, String accountId);

  void update(String budgetId, Budget budget);
  void setThresholdCrossedTimestamp(Budget budget, int thresholdIndex, long crossedAt);
  void incAlertCount(Budget budget, int thresholdIndex);

  List<Budget> list(String accountId);
  List<Budget> listCgBudgets(String accountId);
  List<Budget> list(String accountId, Integer count, Integer startIndex);
  List<Budget> list(String accountId, String viewId);
  int getBudgetCount(String accountId);

  boolean delete(String budgetId, String accountId);

  double getActualCost(Budget budget);

  double getForecastCost(Budget budget);

  QLBudgetDataList getBudgetData(Budget budget);

  QLBudgetTableData getBudgetDetails(Budget budget);

  boolean isStartOfMonth();

  boolean isAlertSentInCurrentMonth(long crossedAt);
}
