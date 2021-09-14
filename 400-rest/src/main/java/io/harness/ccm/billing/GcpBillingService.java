/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.math3.stat.regression.SimpleRegression;

@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public interface GcpBillingService {
  BigDecimal getTotalCost(List<Condition> conditions);
  SimpleRegression getSimpleRegression(List<Condition> conditions, Date startDate, Date endDate);
  BigDecimal getCostTrend(SimpleRegression regression, Date startDate, Date endDate);
  BigDecimal getCostEstimate(SimpleRegression regression, Date startDate, Date endDate);
  GcpBillingEntityStatsDTO getGcpBillingEntityStats(
      List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions);
  GcpBillingTimeSeriesStatsDTO getGcpBillingTimeSeriesStats(
      SqlObject aggregateFunction, List<Object> groupbyObjects, List<Condition> conditions);
}
