package io.harness.ccm.setup.graphql;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCESetupSortCriteria {
  private QLCESetupSortType sortType;
  private QLSortOrder sortOrder;
}
