package io.harness.polling.mapper.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.manifest.HelmChartManifestInfo;
import io.harness.polling.contracts.HelmVersion;
import io.harness.polling.contracts.HttpHelmPayload;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.mapper.PollingInfoBuilder;

@OwnedBy(HarnessTeam.CDC)
public class HttpHelmChartManifestInfoBuilder implements PollingInfoBuilder {
  @Override
  public PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData) {
    HttpHelmPayload httpHelmPayload = pollingPayloadData.getHttpHelmPayload();
    return HelmChartManifestInfo.builder()
        .store(HttpStoreConfig.builder()
                   .connectorRef(ParameterField.<String>builder().value(pollingPayloadData.getConnectorRef()).build())
                   .build())
        .chartName(httpHelmPayload.getChartName())
        .helmVersion(httpHelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                        : io.harness.k8s.model.HelmVersion.V3)
        .build();
  }
}
