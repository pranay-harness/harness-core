package io.harness.polling.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingDocument.PollingDocumentBuilder;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.PollingType;
import io.harness.polling.bean.manifest.HelmChartManifestInfo;
import io.harness.polling.contracts.GcsHelmPayload;
import io.harness.polling.contracts.HelmVersion;
import io.harness.polling.contracts.HttpHelmPayload;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.contracts.Qualifier;
import io.harness.polling.contracts.S3HelmPayload;

import java.util.Collections;

@OwnedBy(HarnessTeam.CDC)
public class PollingRequestToPollingDocumentMapper {
  public PollingDocument toPollingDocument(PollingItem pollingItem) {
    Qualifier qualifier = pollingItem.getQualifier();
    PollingInfo pollingInfo = null;
    PollingDocumentBuilder pollingDocumentBuilder = PollingDocument.builder();
    switch (pollingItem.getCategory()) {
      case MANIFEST:
        pollingDocumentBuilder.pollingType(PollingType.MANIFEST);
        pollingInfo = getManifestPollingInfo(
            pollingItem.getPollingPayloadData(), pollingItem.getPollingPayloadData().getConnectorRef());
        break;
      case ARTIFACT:
        pollingDocumentBuilder.pollingType(PollingType.ARTIFACT);
        break;
      default:
        throw new InvalidRequestException("Unsupported category type " + pollingItem.getCategory());
    }

    return pollingDocumentBuilder.accountId(qualifier.getAccountId())
        .orgIdentifier(qualifier.getOrganizationId())
        .projectIdentifier(qualifier.getProjectId())
        .signatures(Collections.singletonList(qualifier.getSignature()))
        .pollingInfo(pollingInfo)
        .failedAttempts(0)
        .build();
  }

  private PollingInfo getManifestPollingInfo(PollingPayloadData pollingPayloadData, String connectorRef) {
    switch (pollingPayloadData.getType()) {
      case HTTP_HELM:
        HttpHelmPayload httpHelmPayload = pollingPayloadData.getHttpHelmPayload();
        return HelmChartManifestInfo.builder()
            .store(HttpStoreConfig.builder()
                       .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
                       .build())
            .chartName(httpHelmPayload.getChartName())
            .helmVersion(httpHelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                            : io.harness.k8s.model.HelmVersion.V3)
            .build();
      case S3_HELM:
        S3HelmPayload s3HelmPayload = pollingPayloadData.getS3HelmPayload();
        return HelmChartManifestInfo.builder()
            .store(S3StoreConfig.builder()
                       .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
                       .bucketName(ParameterField.<String>builder().value(s3HelmPayload.getBucketName()).build())
                       .region(ParameterField.<String>builder().value(s3HelmPayload.getRegion()).build())
                       .folderPath(ParameterField.<String>builder().value(s3HelmPayload.getFolderPath()).build())
                       .build())
            .chartName(s3HelmPayload.getChartName())
            .helmVersion(s3HelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                          : io.harness.k8s.model.HelmVersion.V3)
            .build();
      case GCS_HELM:
        GcsHelmPayload gcsHelmPayload = pollingPayloadData.getGcsHelmPayload();
        return HelmChartManifestInfo.builder()
            .store(GcsStoreConfig.builder()
                       .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
                       .bucketName(ParameterField.<String>builder().value(gcsHelmPayload.getBucketName()).build())
                       .folderPath(ParameterField.<String>builder().value(gcsHelmPayload.getFolderPath()).build())
                       .build())
            .chartName(gcsHelmPayload.getChartName())
            .helmVersion(gcsHelmPayload.getHelmVersion() == HelmVersion.V2 ? io.harness.k8s.model.HelmVersion.V2
                                                                           : io.harness.k8s.model.HelmVersion.V3)
            .build();
      default:
        throw new InvalidRequestException("Unsupported manifest type " + pollingPayloadData.getType());
    }
  }
}
