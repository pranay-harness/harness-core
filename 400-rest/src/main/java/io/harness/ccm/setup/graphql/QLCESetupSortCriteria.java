package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class QLCESetupSortCriteria {
  private QLCESetupSortType sortType;
  private QLSortOrder sortOrder;
}
