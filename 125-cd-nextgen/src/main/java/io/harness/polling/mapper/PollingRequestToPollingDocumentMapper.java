package io.harness.polling.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.contracts.HttpHelmPayload;
import io.harness.polling.contracts.PayloadType;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.Qualifier;

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
            .build();
      default:
        throw new InvalidRequestException("Unsupported manifest type " + payloadType.getType());
    }
  }
}
