/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
import java.util.Optional;

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
    Optional<String> perspectiveId = viewsQueryHelper.getPerspectiveIdFromMetadataFilter(filters);
    List<ViewRule> viewRuleList = new ArrayList<>();
    if (perspectiveId.isPresent()) {
      CEView perspective = viewService.get(perspectiveId.get());
      viewRuleList = perspective.getViewRules();
    }
    // Todo: filter out irrelevant filters and group by here
    // Todo: Add accountId filter here

    String query = viewsQueryBuilder.getAnomalyQuery(viewRuleList, idFilters, timeFilters);
    return anomalyDao.getAnomalyData(query);
  }
}
