package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewChartType;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewType;

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
  Long createdAt;
  Long lastUpdatedAt;
  ViewChartType chartType;
  ViewType viewType;
  ViewState viewState;
}
