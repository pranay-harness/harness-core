package software.wings.helpers.ext.pcf.request;

import io.harness.delegate.task.pcf.PcfManifestFileData;

import software.wings.helpers.ext.pcf.PcfRequestConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfCreateApplicationRequestData {
  private PcfRequestConfig pcfRequestConfig;
  private String finalManifestYaml;
  private PcfManifestFileData pcfManifestFileData;
  private String manifestFilePath;
  private String configPathVar;
  private String artifactPath;
  private PcfCommandSetupRequest setupRequest;
  private String newReleaseName;
  private boolean varsYmlFilePresent;
}
