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
