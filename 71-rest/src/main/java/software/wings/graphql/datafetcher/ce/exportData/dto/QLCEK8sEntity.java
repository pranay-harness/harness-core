package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CEK8sEntityKeys")
public class QLCEK8sEntity {
  String namespace;
  String workload;
  String node;
  String pod;
}