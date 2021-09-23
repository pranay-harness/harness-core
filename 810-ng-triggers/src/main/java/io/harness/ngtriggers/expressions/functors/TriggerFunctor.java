package io.harness.ngtriggers.expressions.functors;

import static io.harness.ngtriggers.Constants.EVENT_PAYLOAD;
import static io.harness.ngtriggers.Constants.PAYLOAD;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.expression.LateBindingValue;
import io.harness.ngtriggers.helpers.TriggerHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerFunctor implements LateBindingValue {
  private final Ambiance ambiance;
  private final PlanExecutionMetadataService planExecutionMetadataService;

  public TriggerFunctor(Ambiance ambiance, PlanExecutionMetadataService planExecutionMetadataService) {
    this.ambiance = ambiance;
    this.planExecutionMetadataService = planExecutionMetadataService;
  }

  @Override
  public Object bind() {
    PlanExecutionMetadata metadata =
        planExecutionMetadataService.findByPlanExecutionId(ambiance.getPlanExecutionId())
            .orElseThrow(()
                             -> new IllegalStateException(
                                 "No Metadata present for planExecution :" + ambiance.getPlanExecutionId()));
    Map<String, Object> jsonObject = TriggerHelper.buildJsonObjectFromAmbiance(metadata.getTriggerPayload());

    if (isNotBlank(metadata.getTriggerJsonPayload())) {
      jsonObject.put(EVENT_PAYLOAD, metadata.getTriggerJsonPayload());
      // payload
      try {
        jsonObject.put(PAYLOAD, JsonPipelineUtils.read(metadata.getTriggerJsonPayload(), HashMap.class));
      } catch (IOException e) {
        throw new InvalidRequestException("Event payload could not be converted to a hashmap");
      }
    }
    return jsonObject;
  }
}
