package io.harness.ccm.views.graphql;

import io.harness.ccm.billing.TimeSeriesDataPoints;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLViewTimeSeriesData implements QLData {
  List<TimeSeriesDataPoints> stats;
}
