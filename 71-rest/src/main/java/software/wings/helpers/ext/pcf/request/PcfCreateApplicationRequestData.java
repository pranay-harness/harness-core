package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.pcf.PcfRequestConfig;

@Data
@Builder
public class PcfCreateApplicationRequestData {
  private PcfRequestConfig pcfRequestConfig;
  private String finalManifestYaml;
  private String manifestFilePath;
  private String configPathVar;
  private String artifactPath;
  private PcfCommandSetupRequest setupRequest;
  private String newReleaseName;
}
