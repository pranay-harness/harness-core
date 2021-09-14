/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.service.intf;

import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetData;

import java.util.List;

public interface BudgetService {
  String create(Budget budgetRecord);
  String clone(String budgetId, String budgetName, String accountId);

  Budget get(String budgetId, String accountId);

  void update(String budgetId, Budget budget);

  List<Budget> list(String accountId);
  List<Budget> list(String accountId, String viewId);

  boolean delete(String budgetId, String accountId);

  Double getLastMonthCostForPerspective(String accountId, String perspectiveId);
  Double getForecastCostForPerspective(String accountId, String perspectiveId);

  BudgetData getBudgetTimeSeriesStats(Budget budget);
}
