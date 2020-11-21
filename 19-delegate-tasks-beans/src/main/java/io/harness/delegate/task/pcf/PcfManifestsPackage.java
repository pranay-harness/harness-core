package io.harness.delegate.task.pcf;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfManifestsPackage {
  private String manifestYml;
  private String autoscalarManifestYml;
  private List<String> variableYmls;
}
