package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CEK8sEntityKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCEK8sEntity {
  String namespace;
  String workload;
  String workloadType;
  String node;
  String pod;
  List<QLCEK8sLabels> selectedLabels;
}
