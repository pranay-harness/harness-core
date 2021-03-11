package io.harness.ccm.views.service.impl;

import io.harness.ccm.anomaly.dao.AnomalyEntityDao;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewRule;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.anomalydetection.ADViewsQueryBuilder;
import io.harness.ccm.views.service.ViewAnomalyService;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewAnomalyServiceImpl extends ViewsBillingServiceImpl implements ViewAnomalyService {
  @Inject ADViewsQueryBuilder queryBuilder;
  @Inject AnomalyEntityDao anomalyEntityDao;
  @Override
  public List<AnomalyEntity> list(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy) {
    // separate filters from wrapper
    List<QLCEViewFilter> idFilters = getIdFilters(filters);
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);

    List<ViewRule> viewRules = new ArrayList<>();
    List<QLCEViewRule> rules = getRuleFilters(filters);
    if (!rules.isEmpty()) {
      for (QLCEViewRule rule : rules) {
        viewRules.add(convertQLCEViewRuleToViewRule(rule));
      }
    }

    List<QLCEViewAggregation> aggregationList = Collections.emptyList();
    List<QLCEViewSortCriteria> sortList = Collections.emptyList();

    SelectQuery query;
    List<AnomalyEntity> anomalies = new ArrayList<>();
    try {
      query = queryBuilder.getQuery(viewRules, idFilters, timeFilters, groupBy, aggregationList, sortList);
      log.info("Prepared Query for view : {}", query.toString());
      anomalies = anomalyEntityDao.list(query.toString());
    } catch (Exception e) {
      log.error("Error while fetching anomalies for view , Exception : {} ", e.toString());
    }
    return anomalies;
  }
}
