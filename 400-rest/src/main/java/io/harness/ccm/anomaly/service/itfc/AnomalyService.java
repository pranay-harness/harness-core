package io.harness.ccm.anomaly.service.itfc;

import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;

import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;

import java.time.Instant;
import java.util.List;

public interface AnomalyService {
  /* anomalies on date */
  List<AnomalyEntity> list(String account, Instant date);
  /* anomalies in [date1,date2] */
  List<AnomalyEntity> list(String account, Instant from, Instant to);
  List<AnomalyEntity> listK8s(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy);
  List<AnomalyEntity> listCloud(String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy);
  List<AnomalyEntity> listOverview(String accountId, List<QLBillingDataFilter> filters);
  List<AnomalyEntity> listViews(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort);
  void delete(List<String> ids, Instant date);
  void insert(List<? extends AnomalyEntity> anomalies);
  AnomalyEntity update(AnomalyEntity anomaly);
}
