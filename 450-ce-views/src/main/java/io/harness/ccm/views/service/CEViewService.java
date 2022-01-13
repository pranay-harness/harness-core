/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.service;

import io.harness.ccm.views.dto.DefaultViewIdDto;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.graphql.QLCEView;

import com.google.cloud.bigquery.BigQuery;
import java.util.List;

public interface CEViewService {
  CEView save(CEView ceView);

  double getActualCostForPerspectiveBudget(String accountId, String perspectiveId);

  CEView get(String uuid);
  CEView update(CEView ceView);
  CEView updateTotalCost(CEView ceView, BigQuery bigQuery, String cloudProviderTableName);
  boolean delete(String uuid, String accountId);
  List<QLCEView> getAllViews(String accountId, boolean includeDefault);
  List<CEView> getViewByState(String accountId, ViewState viewState);
  void createDefaultView(String accountId, ViewFieldIdentifier viewFieldIdentifier);
  DefaultViewIdDto getDefaultViewIds(String accountId);

  Double getLastMonthCostForPerspective(String accountId, String perspectiveId);
  Double getForecastCostForPerspective(String accountId, String perspectiveId);

  void updateDefaultClusterViewVisualization(String viewId);
}
