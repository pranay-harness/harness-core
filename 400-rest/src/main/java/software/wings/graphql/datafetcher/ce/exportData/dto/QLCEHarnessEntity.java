package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEHarnessEntityKeys")
public class QLCEHarnessEntity {
  String application;
  String service;
  String environment;
}
