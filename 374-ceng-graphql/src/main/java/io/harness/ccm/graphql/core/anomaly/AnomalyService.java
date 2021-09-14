/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.core.anomaly;

import io.harness.ccm.commons.dao.anomaly.AnomalyDao;
import io.harness.ccm.commons.entities.anomaly.AnomalyDataList;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.service.CEViewService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AnomalyService {
  @Inject AnomalyDao anomalyDao;
  @Inject ViewsQueryBuilder viewsQueryBuilder;
  @Inject ViewsQueryHelper viewsQueryHelper;
  @Inject CEViewService viewService;

  public AnomalyDataList getAnomalyDataForPerspective(
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, String accountId) {
    List<QLCEViewTimeFilter> timeFilters = viewsQueryHelper.getTimeFilters(filters);
    List<QLCEViewFilter> idFilters = viewsQueryHelper.getIdFilters(filters);
    String perspectiveId = viewsQueryHelper.getPerspectiveIdFromMetadataFilter(filters);
    List<ViewRule> viewRuleList = new ArrayList<>();
    if (perspectiveId != null) {
      CEView perspective = viewService.get(perspectiveId);
      viewRuleList = perspective.getViewRules();
    }
    // Todo: filter out irrelevant filters and group by here
    // Todo: Add accountId filter here

    String query = viewsQueryBuilder.getAnomalyQuery(viewRuleList, idFilters, timeFilters);
    return anomalyDao.getAnomalyData(query);
  }
}
