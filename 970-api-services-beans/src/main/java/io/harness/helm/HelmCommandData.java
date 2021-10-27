package io.harness.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class HelmCommandData {
  private boolean isRepoConfigNull;
  @Default private boolean isHelmCmdFlagsNull = true;
  private String chartUrl;
  private String chartVersion;
  private String chartName;
  private String workingDir;
  private String commandFlags;
  private String kubeConfigLocation;
  private String releaseName;
  private String repoName;
  private HelmVersion helmVersion;
  @Default Map<HelmSubCommandType, String> valueMap = new HashMap<>();
  private List<String> yamlFiles;
  private LogCallback logCallback;
  private String namespace;
  private Integer prevReleaseVersion;
  private Integer newReleaseVersion;
  private long timeOutInMillis;
  // field below unique to HelmRollbackCommandRequest
  private Integer rollBackVersion;
}
