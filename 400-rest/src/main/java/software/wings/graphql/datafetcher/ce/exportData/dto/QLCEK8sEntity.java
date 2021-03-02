package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CEK8sEntityKeys")
@TargetModule(Module._375_CE_GRAPHQL)
public class QLCEK8sEntity {
  String namespace;
  String workload;
  String workloadType;
  String node;
  String pod;
  List<QLCEK8sLabels> selectedLabels;
}
