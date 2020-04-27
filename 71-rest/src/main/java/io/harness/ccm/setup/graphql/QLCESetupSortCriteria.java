package io.harness.ccm.setup.graphql;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

@Value
@Builder
public class QLCESetupSortCriteria {
  private QLCESetupSortType sortType;
  private QLSortOrder sortOrder;
}