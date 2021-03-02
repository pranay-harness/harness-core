package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEEcsEntityKeys")
@TargetModule(Module._375_CE_GRAPHQL)
public class QLCEEcsEntity {
  String launchType;
  String service;
  String taskId;
}
