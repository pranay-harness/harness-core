/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.mapper.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.manifest.HelmChartManifestInfo;
import io.harness.polling.contracts.GcsHelmPayload;
import io.harness.polling.contracts.HelmVersion;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.mapper.PollingInfoBuilder;

@OwnedBy(HarnessTeam.CDC)
public class GcsHelmChartManifestInfoBuilder implements PollingInfoBuilder {
  @Override
  public PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData) {
    GcsHelmPayload gcsHelmPayload = pollingPayloadData.getGcsHelmPayload();
    return HelmChartManifestInfo.builder()
        .store(GcsStoreConfig.builder()
                   .connectorRef(ParameterField.<String>builder().value(pollingPayloadData.getConnectorRef()).build())
                   .bucketName(ParameterField.<String>builder().value(gcsHelmPayload.getBucketName()).build())
                   .folderPath(ParameterField.<String>builder().value(gcsHelmPayload.getFolderPath()).build())
                   .build())
        .chartName(gcsHelmPayload.getChartName())
        .helmVersion(gcsHelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                       : io.harness.k8s.model.HelmVersion.V3)
        .build();
  }
}
