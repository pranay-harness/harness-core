package io.harness.ccm.views.service;

import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;

import java.util.List;

public interface ViewAnomalyService {
  List<AnomalyEntity> list(
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction);
}
