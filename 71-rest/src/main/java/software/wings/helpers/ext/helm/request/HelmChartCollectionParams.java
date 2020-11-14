package software.wings.helpers.ext.helm.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
@OwnedBy(CDC)
public class HelmChartCollectionParams implements ManifestCollectionParams {
  private String accountId;
  private String appId;
  private String appManifestId;
  private String serviceId;
  private HelmChartConfigParams helmChartConfigParams;
  private Set<String> publishedVersions;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return helmChartConfigParams.fetchRequiredExecutionCapabilities();
  }
}
