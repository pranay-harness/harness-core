package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEEcsEntityKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCEEcsEntity {
  String launchType;
  String service;
  String taskId;
}
