package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
