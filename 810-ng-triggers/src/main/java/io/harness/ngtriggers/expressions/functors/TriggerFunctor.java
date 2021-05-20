package io.harness.ngtriggers.expressions.functors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingValue;
import io.harness.ngtriggers.helpers.TriggerAmbianceHelper;
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

    ParsedPayload parsedPayload = TriggerAmbianceHelper.getParsedPayload(ambiance);
    String payloadFromEvent = TriggerAmbianceHelper.getEventPayload(ambiance);
    // branchesxv
    switch (parsedPayload.getPayloadCase()) {
      case PR:
        jsonObject.put("branch", parsedPayload.getPr().getPr().getTarget());
        jsonObject.put("targetBranch", parsedPayload.getPr().getPr().getTarget());
        jsonObject.put("sourceBranch", parsedPayload.getPr().getPr().getSource());
        jsonObject.put("event", "PR");
        jsonObject.put("prNumber", Long.toString(parsedPayload.getPr().getPr().getNumber()));
        jsonObject.put("commitSha", parsedPayload.getPr().getPr().getSha());
        jsonObject.put("type", "WEBHOOK");
        break;
      case PUSH:
        jsonObject.put("branch", parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put("targetBranch", parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put("commitSha", parsedPayload.getPush().getAfter());
        jsonObject.put("event", "PUSH");
        jsonObject.put("type", "WEBHOOK");
        break;
      default:
        if (isEmpty(payloadFromEvent)) {
          jsonObject.put("type", "SCHEDULED");
        } else {
          jsonObject.put("type", "CUSTOM");
        }
        break;
    }

    // headers
    jsonObject.put("header", TriggerAmbianceHelper.getHeadersMap(ambiance));
    jsonObject.put("eventPayload", payloadFromEvent);
    // payload
    try {
      jsonObject.put("payload", JsonPipelineUtils.read(TriggerAmbianceHelper.getEventPayload(ambiance), HashMap.class));
    } catch (IOException e) {
      throw new InvalidRequestException("Event payload could not be converted to a hashmap");
    }
    return jsonObject;
  }
}
