package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEHarnessEntityKeys")
@TargetModule(Module._375_CE_GRAPHQL)
public class QLCEHarnessEntity {
  String application;
  String service;
  String environment;
}
