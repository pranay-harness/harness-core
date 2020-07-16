package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEEcsEntityKeys")
public class QLCEEcsEntity {
  String launchType;
  String service;
  String taskId;
}
