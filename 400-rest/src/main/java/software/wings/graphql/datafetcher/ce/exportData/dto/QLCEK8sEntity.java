package software.wings.graphql.datafetcher.ce.exportData.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

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
