package io.harness.ngpipeline.expressions.functors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingValue;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerFunctor implements LateBindingValue {
  private final Ambiance ambiance;

  public TriggerFunctor(Ambiance ambiance) {
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    Map<String, Object> jsonObject = new HashMap<>();

    ParsedPayload parsedPayload = ambiance.getMetadata().getTriggerPayload().getParsedPayload();
    // branchesxv
    switch (parsedPayload.getPayloadCase()) {
      case PR:
        jsonObject.put("branch", parsedPayload.getPr().getPr().getTarget());
        jsonObject.put("targetBranch", parsedPayload.getPr().getPr().getTarget());
        jsonObject.put("sourceBranch", parsedPayload.getPr().getPr().getSource());
        jsonObject.put("event", "PR");
        jsonObject.put("type", "WEBHOOK");
        break;
      case PUSH:
        jsonObject.put("branch", parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put("targetBranch", parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put("event", "PUSH");
        jsonObject.put("type", "WEBHOOK");
        break;
      default:
        if (isEmpty(AmbianceHelper.getEventPayload(ambiance))) {
          jsonObject.put("type", "SCHEDULED");
        } else {
          jsonObject.put("type", "CUSTOM");
        }
        break;
    }

    // headers
    jsonObject.put("header", ambiance.getMetadata().getTriggerPayload().getHeadersMap());
    // payload
    try {
      jsonObject.put("payload", JsonPipelineUtils.read(AmbianceHelper.getEventPayload(ambiance), HashMap.class));
    } catch (IOException e) {
      throw new InvalidRequestException("Event payload could not be converted to a hashmap");
    }
    return jsonObject;
  }
}
