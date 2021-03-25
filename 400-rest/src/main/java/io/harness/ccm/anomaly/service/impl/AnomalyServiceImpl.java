package io.harness.ccm.anomaly.service.impl;

import io.harness.ccm.anomaly.dao.AnomalyEntityDao;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.service.AnomalyDataQueryBuilder;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;

import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnomalyServiceImpl implements AnomalyService {
  @Autowired @Inject private AnomalyEntityDao anomalyEntityDao;

  @Override
  public List<AnomalyEntity> list(String account, Instant date) {
    SelectQuery query = new SelectQuery();
    query.addAllTableColumns(AnomalyEntity.AnomaliesDataTableSchema.table);
    AnomalyDataQueryBuilder.addAccountFilter(query, account);
    AnomalyDataQueryBuilder.isViews(query, false);
    query.addCondition(
        BinaryCondition.equalTo(AnomalyEntity.AnomaliesDataTableSchema.anomalyTime, date.truncatedTo(ChronoUnit.DAYS)));
    return anomalyEntityDao.list(query.validate().toString());
  }

  @Override
  public List<AnomalyEntity> list(String account, Instant from, Instant to) {
    SelectQuery query = new SelectQuery();
    query.addAllTableColumns(AnomalyEntity.AnomaliesDataTableSchema.table);
    AnomalyDataQueryBuilder.addAccountFilter(query, account);
    AnomalyDataQueryBuilder.isViews(query, false);
    query.addCondition(BinaryCondition.lessThanOrEq(
        AnomalyEntity.AnomaliesDataTableSchema.anomalyTime, to.truncatedTo(ChronoUnit.DAYS)));
    query.addCondition(BinaryCondition.greaterThanOrEq(
        AnomalyEntity.AnomaliesDataTableSchema.anomalyTime, from.truncatedTo(ChronoUnit.DAYS)));
    return anomalyEntityDao.list(query.validate().toString());
  }

  public List<AnomalyEntity> listK8s(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    try {
      String queryStatement = AnomalyDataQueryBuilder.formK8SQuery(accountId, filters, groupBy);
      return anomalyEntityDao.list(queryStatement);
    } catch (Exception e) {
      log.error("Exception occurred in listK8s: [{}]", e.toString());
      return Collections.emptyList();
    }
  }

  public List<AnomalyEntity> listCloud(
      String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    try {
      String queryStatement = AnomalyDataQueryBuilder.formCloudQuery(accountId, filters, groupBy);
      return anomalyEntityDao.list(queryStatement);
    } catch (Exception e) {
      log.error("Exception occurred in listCloud: [{}]", e.toString());
      return Collections.emptyList();
    }
  }
  public List<AnomalyEntity> listOverview(String accountId, List<QLBillingDataFilter> filters) {
    try {
      String queryStatement = AnomalyDataQueryBuilder.overviewQuery(accountId, filters);
      return anomalyEntityDao.list(queryStatement);
    } catch (Exception e) {
      log.error("Exception occurred in listOverview: [{}]", e.toString());
      return Collections.emptyList();
    }
  }

  @Override
  public List<AnomalyEntity> listViews(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort) {
    try {
      String queryStatement = AnomalyDataQueryBuilder.formViewsQuery(accountId, filters);
      return anomalyEntityDao.list(queryStatement);
    } catch (Exception e) {
      log.error("Exception occurred in listOverview: [{}]", e.toString());
      return Collections.emptyList();
    }
  }

  @Override
  public void delete(List<String> ids, Instant date) {
    anomalyEntityDao.delete(ids, date);
  }

  @Override
  public void insert(List<? extends AnomalyEntity> anomalies) {
    anomalyEntityDao.insert(anomalies);
  }

  public AnomalyEntity update(AnomalyEntity anomaly) {
    return anomalyEntityDao.update(anomaly);
  }
}
