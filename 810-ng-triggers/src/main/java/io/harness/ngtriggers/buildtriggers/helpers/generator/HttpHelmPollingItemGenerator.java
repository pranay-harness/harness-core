package io.harness.ngtriggers.buildtriggers.helpers.generator;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.polling.contracts.HttpHelmPayload;
import io.harness.polling.contracts.PayloadType;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.Type;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PIPELINE)
public class HttpHelmPollingItemGenerator implements PollingItemGenerator {
  @Inject BuildTriggerHelper buildTriggerHelper;

  @Override
  public PollingItem generatePollingItem(BuildTriggerOpsData buildTriggerOpsData) {
    NGTriggerEntity ngTriggerEntity = buildTriggerOpsData.getTriggerDetails().getNgTriggerEntity();

    PollingItem.Builder builder = getBaseInitializedPollingItem(ngTriggerEntity);

    String connectorRef = buildTriggerOpsData.getPipelineBuildSpecMap().containsKey("spec.store.spec.connectorRef")
        ? ((JsonNode) buildTriggerOpsData.getPipelineBuildSpecMap().get("spec.store.spec.connectorRef")).asText()
        : EMPTY;
    if (isBlank(connectorRef) || "<+input>".equals(connectorRef)) {
      connectorRef = buildTriggerHelper.fetchValueFromJsonNode(
          "spec.store.spec.connectorRef", buildTriggerOpsData.getTriggerSpecMap());
    }

    String chartName = buildTriggerOpsData.getPipelineBuildSpecMap().containsKey("spec.chartName")
        ? ((JsonNode) buildTriggerOpsData.getPipelineBuildSpecMap().get("spec.chartName")).asText()
        : EMPTY;
    if (isBlank(chartName) || "<+input>".equals(chartName)) {
      chartName = buildTriggerHelper.fetchValueFromJsonNode("spec.chartName", buildTriggerOpsData.getTriggerSpecMap());
    }

    return builder.setConnectorRef(connectorRef)
        .setPayloadType(PayloadType.newBuilder()
                            .setType(Type.HTTP_HELM)
                            .setHttpHelmPayload(HttpHelmPayload.newBuilder().setChartName(chartName).build())
                            .build())
        .build();
  }
}
