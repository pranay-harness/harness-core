package io.harness.ccm.anomaly.service.impl;

import io.harness.ccm.anomaly.dao.AnomalyEntityDao;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;

import software.wings.graphql.datafetcher.anomaly.AnomalyDataQueryBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnomalyServiceImpl implements AnomalyService {
  @Autowired @Inject private AnomalyEntityDao anomalyEntityDao;

  @Override
  public List<AnomalyEntity> list(String account, Instant date) {
    SelectQuery query = new SelectQuery();
    query.addAllTableColumns(AnomalyEntity.AnomaliesDataTableSchema.table);
    addAccountIdFilter(account, query);
    query.addCondition(
        BinaryCondition.equalTo(AnomalyEntity.AnomaliesDataTableSchema.anomalyTime, date.truncatedTo(ChronoUnit.DAYS)));
    return anomalyEntityDao.list(query.validate().toString());
  }

  public List<AnomalyEntity> listK8s(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    String queryStatement = AnomalyDataQueryBuilder.formK8SQuery(accountId, filters, groupBy);
    return anomalyEntityDao.list(queryStatement);
  }

  public List<AnomalyEntity> listCloud(
      String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    String queryStatement = AnomalyDataQueryBuilder.formCloudQuery(accountId, filters, groupBy);
    return anomalyEntityDao.list(queryStatement);
  }
  public List<AnomalyEntity> listOverview(String accountId, List<QLBillingDataFilter> filters) {
    String queryStatement = AnomalyDataQueryBuilder.overviewQuery(accountId, filters);
    return anomalyEntityDao.list(queryStatement);
  }

  @Override
  public void delete(List<String> ids, Instant date) {
    anomalyEntityDao.delete(ids, date);
  }

  @Override
  public void insert(List<AnomalyEntity> anomalies) {
    anomalyEntityDao.insert(anomalies);
  }

  private void addAccountIdFilter(String accountId, SelectQuery query) {
    query.addCondition(BinaryCondition.equalTo(AnomalyEntity.AnomaliesDataTableSchema.accountId, accountId));
  }

  public AnomalyEntity update(AnomalyEntity anomaly) {
    return anomalyEntityDao.update(anomaly);
  }
}
