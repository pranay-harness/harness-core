package io.harness.polling.mapper.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.artifact.EcrArtifactInfo;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.mapper.PollingInfoBuilder;

@OwnedBy(HarnessTeam.CDC)
public class EcrArtifactInfoBuilder implements PollingInfoBuilder {
  @Override
  public PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData) {
    return EcrArtifactInfo.builder()
        .connectorRef(pollingPayloadData.getConnectorRef())
        .imagePath(pollingPayloadData.getEcrPayload().getImagePath())
        .region(pollingPayloadData.getEcrPayload().getRegion())
        .build();
  }
}
