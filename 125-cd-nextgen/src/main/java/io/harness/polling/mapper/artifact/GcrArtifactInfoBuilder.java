package io.harness.polling.mapper.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.artifact.GcrArtifactInfo;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.mapper.PollingInfoBuilder;

@OwnedBy(HarnessTeam.CDC)
public class GcrArtifactInfoBuilder implements PollingInfoBuilder {
  @Override
  public PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData) {
    return GcrArtifactInfo.builder()
        .connectorRef(pollingPayloadData.getConnectorRef())
        .imagePath(pollingPayloadData.getGcrPayload().getImagePath())
        .registryHostname(pollingPayloadData.getGcrPayload().getRegistryHostname())
        .build();
  }
}
