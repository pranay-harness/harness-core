package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CEK8sEntityKeys")
public class QLCEK8sEntity {
  String namespace;
  String workload;
  String node;
  String pod;
  List<QLCEK8sLabels> selectedLabels;
}
