package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEHarnessEntityKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
public class QLCEHarnessEntity {
  String application;
  String service;
  String environment;
}
