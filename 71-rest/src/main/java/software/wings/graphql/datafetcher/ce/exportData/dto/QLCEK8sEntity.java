package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCEK8sEntity {
  String namespace;
  String workload;
  String nodeId;
  String podId;
}