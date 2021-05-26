package io.harness.ngtriggers.expressions.functors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.Constants.BRANCH;
import static io.harness.ngtriggers.Constants.COMMIT_SHA;
import static io.harness.ngtriggers.Constants.CUSTOM_TYPE;
import static io.harness.ngtriggers.Constants.EVENT;
import static io.harness.ngtriggers.Constants.EVENT_PAYLOAD;
import static io.harness.ngtriggers.Constants.GIT_USER;
import static io.harness.ngtriggers.Constants.HEADER;
import static io.harness.ngtriggers.Constants.PAYLOAD;
import static io.harness.ngtriggers.Constants.PR;
import static io.harness.ngtriggers.Constants.PR_NUMBER;
import static io.harness.ngtriggers.Constants.PUSH;
import static io.harness.ngtriggers.Constants.REPO_URL;
import static io.harness.ngtriggers.Constants.SCHEDULED_TYPE;
import static io.harness.ngtriggers.Constants.SOURCE_BRANCH;
import static io.harness.ngtriggers.Constants.TARGET_BRANCH;
import static io.harness.ngtriggers.Constants.TYPE;
import static io.harness.ngtriggers.Constants.WEBHOOK_TYPE;

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
        jsonObject.put(BRANCH, parsedPayload.getPr().getPr().getTarget());
        jsonObject.put(TARGET_BRANCH, parsedPayload.getPr().getPr().getTarget());
        jsonObject.put(SOURCE_BRANCH, parsedPayload.getPr().getPr().getSource());
        jsonObject.put(EVENT, PR);
        jsonObject.put(PR_NUMBER, Long.toString(parsedPayload.getPr().getPr().getNumber()));
        jsonObject.put(COMMIT_SHA, parsedPayload.getPr().getPr().getSha());
        jsonObject.put(TYPE, WEBHOOK_TYPE);
        jsonObject.put(REPO_URL, parsedPayload.getPr().getRepo().getLink());
        jsonObject.put(GIT_USER, parsedPayload.getPr().getSender().getLogin());
        break;
      case PUSH:
        jsonObject.put(BRANCH, parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put(TARGET_BRANCH, parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put(COMMIT_SHA, parsedPayload.getPush().getAfter());
        jsonObject.put(EVENT, PUSH);
        jsonObject.put(TYPE, WEBHOOK_TYPE);
        jsonObject.put(REPO_URL, parsedPayload.getPush().getRepo().getLink());
        jsonObject.put(GIT_USER, parsedPayload.getPush().getSender().getLogin());
        break;
      default:
        if (isEmpty(payloadFromEvent)) {
          jsonObject.put(TYPE, SCHEDULED_TYPE);
        } else {
          jsonObject.put(TYPE, CUSTOM_TYPE);
        }
        break;
    }

    // headers
    jsonObject.put(HEADER, TriggerAmbianceHelper.getHeadersMap(ambiance));
    jsonObject.put(EVENT_PAYLOAD, payloadFromEvent);
    // payload
    try {
      jsonObject.put(PAYLOAD, JsonPipelineUtils.read(TriggerAmbianceHelper.getEventPayload(ambiance), HashMap.class));
    } catch (IOException e) {
      throw new InvalidRequestException("Event payload could not be converted to a hashmap");
    }
    return jsonObject;
  }
}
