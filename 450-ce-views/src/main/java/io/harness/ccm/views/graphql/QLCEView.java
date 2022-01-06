/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewChartType;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewTimeRangeType;
import io.harness.ccm.views.entities.ViewType;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEView {
  String id;
  String name;
  double totalCost;
  String createdBy;
  Long createdAt;
  Long lastUpdatedAt;
  ViewChartType chartType;
  ViewType viewType;
  ViewState viewState;

  QLCEViewField groupBy;
  ViewTimeRangeType timeRange;
  List<ViewFieldIdentifier> dataSources;
  boolean isReportScheduledConfigured;
}
