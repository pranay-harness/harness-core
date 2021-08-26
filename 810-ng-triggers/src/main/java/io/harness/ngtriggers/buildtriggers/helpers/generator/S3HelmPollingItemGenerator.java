package io.harness.ngtriggers.buildtriggers.helpers.generator;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.polling.contracts.HelmVersion;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.contracts.S3HelmPayload;
import io.harness.polling.contracts.Type;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PIPELINE)
public class S3HelmPollingItemGenerator implements PollingItemGenerator {
  @Inject BuildTriggerHelper buildTriggerHelper;

  @Override
  public PollingItem generatePollingItem(BuildTriggerOpsData buildTriggerOpsData) {
    NGTriggerEntity ngTriggerEntity = buildTriggerOpsData.getTriggerDetails().getNgTriggerEntity();

    PollingItem.Builder builder = getBaseInitializedPollingItem(ngTriggerEntity);

    String connectorRef =
        buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.store.spec.connectorRef");
    String chartName = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.chartName");
    String helmVersion = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.helmVersion");
    String folderPath =
        buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.store.spec.folderPath");
    String region = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.store.spec.region");
    String bucketName =
        buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.store.spec.bucketName");

    return builder
        .setPollingPayloadData(PollingPayloadData.newBuilder()
                                   .setConnectorRef(connectorRef)
                                   .setType(Type.S3_HELM)
                                   .setS3HelmPayload(S3HelmPayload.newBuilder()
                                                         .setChartName(chartName)
                                                         .setHelmVersion(HelmVersion.valueOf(helmVersion))
                                                         .setFolderPath(folderPath)
                                                         .setRegion(region)
                                                         .setBucketName(bucketName)
                                                         .build())
                                   .build())
        .build();
  }
}
