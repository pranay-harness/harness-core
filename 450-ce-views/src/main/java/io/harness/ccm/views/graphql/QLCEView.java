/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
