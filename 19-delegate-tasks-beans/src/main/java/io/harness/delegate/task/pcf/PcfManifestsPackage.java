package io.harness.delegate.task.pcf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PcfManifestsPackage {
  private String manifestYml;
  private String autoscalarManifestYml;
  private List<String> variableYmls;
}
