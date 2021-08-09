package io.harness.polling.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.contracts.GcsHelmPayload;
import io.harness.polling.contracts.HelmVersion;
import io.harness.polling.contracts.HttpHelmPayload;
import io.harness.polling.contracts.PayloadType;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.Qualifier;
import io.harness.polling.contracts.S3HelmPayload;

import java.util.Collections;

@OwnedBy(HarnessTeam.CDC)
public class PollingRequestToPollingDocumentMapper {
  public PollingDocument toPollingDocument(PollingItem pollingItem) {
    Qualifier qualifier = pollingItem.getQualifier();
    PollingInfo pollingInfo;
    switch (pollingItem.getCategory()) {
      case MANIFEST:
        pollingInfo = getManifestPollingInfo(pollingItem.getPayloadType(), pollingItem.getConnectorRef());
        break;
      case ARTIFACT:
      default:
        throw new InvalidRequestException("Unsupported category type " + pollingItem.getCategory());
    }

    return PollingDocument.builder()
        .accountId(qualifier.getAccountId())
        .orgIdentifier(qualifier.getOrganizationId())
        .projectIdentifier(qualifier.getProjectId())
        .signature(Collections.singletonList(qualifier.getSignature()))
        .pollingInfo(pollingInfo)
        .failedAttempts(0)
        .build();
  }

  private PollingInfo getManifestPollingInfo(PayloadType payloadType, String connectorRef) {
    switch (payloadType.getType()) {
      case HTTP_HELM:
        HttpHelmPayload httpHelmPayload = payloadType.getHttpHelmPayload();
        return HelmChartManifestOutcome.builder()
            .store(HttpStoreConfig.builder()
                       .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
                       .build())
            .chartName(ParameterField.<String>builder().value(httpHelmPayload.getChartName()).build())
            .helmVersion(httpHelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                            : io.harness.k8s.model.HelmVersion.V3)
            .build();
      case S3_HELM:
        S3HelmPayload s3HelmPayload = payloadType.getS3HelmPayload();
        return HelmChartManifestOutcome.builder()
            .store(S3StoreConfig.builder()
                       .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
                       .bucketName(ParameterField.<String>builder().value(s3HelmPayload.getBucketName()).build())
                       .region(ParameterField.<String>builder().value(s3HelmPayload.getRegion()).build())
                       .folderPath(ParameterField.<String>builder().value(s3HelmPayload.getFolderPath()).build())
                       .build())
            .chartName(ParameterField.<String>builder().value(s3HelmPayload.getChartName()).build())
            .helmVersion(s3HelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                          : io.harness.k8s.model.HelmVersion.V3)
            .build();
      case GCS_HELM:
        GcsHelmPayload gcsHelmPayload = payloadType.getGcsHelmPayload();
        return HelmChartManifestOutcome.builder()
            .store(GcsStoreConfig.builder()
                       .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
                       .bucketName(ParameterField.<String>builder().value(gcsHelmPayload.getBucketName()).build())
                       .folderPath(ParameterField.<String>builder().value(gcsHelmPayload.getFolderPath()).build())
                       .build())
            .chartName(ParameterField.<String>builder().value(gcsHelmPayload.getChartName()).build())
            .helmVersion(gcsHelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                           : io.harness.k8s.model.HelmVersion.V3)
            .build();
      default:
        throw new InvalidRequestException("Unsupported manifest type " + payloadType.getType());
    }
  }
}
