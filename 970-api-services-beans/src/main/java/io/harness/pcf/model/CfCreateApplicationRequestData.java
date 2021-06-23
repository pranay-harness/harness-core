package io.harness.pcf.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfCreateApplicationRequestData {
  private CfRequestConfig cfRequestConfig;
  private String finalManifestYaml;
  private CfManifestFileData pcfManifestFileData;
  private String manifestFilePath;
  private String configPathVar;
  private String artifactPath;
  private char[] password;
  private boolean dockerBasedDeployment;
  private String newReleaseName;
  private boolean varsYmlFilePresent;
}
