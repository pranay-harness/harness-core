package io.harness.stream;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.serializer.JsonUtils;

import io.harness.delegate.beans.DelegateTaskBroadcast;
import io.harness.delegate.beans.PerpetualTaskBroadcastEvent;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.BroadcastFilterAdapter;
import org.jetbrains.annotations.NotNull;

public class DelegateEventFilter extends BroadcastFilterAdapter {
  @Inject private DelegateService delegateService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;

  @Override
  public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
    AtmosphereRequest req = r.getRequest();
    String delegateId = req.getParameter("delegateId");
    String version = req.getHeader("Version");

    if (message instanceof DelegateTaskBroadcast) {
      DelegateTaskBroadcast broadcast = (DelegateTaskBroadcast) message;

      if (!StringUtils.equals(version, broadcast.getVersion())) {
        return abort(message);
      }

      if (isNotBlank(broadcast.getPreAssignedDelegateId())
          && !StringUtils.equals(broadcast.getPreAssignedDelegateId(), delegateId)) {
        return abort(message);
      }

      if (isNotEmpty(broadcast.getAlreadyTriedDelegates())
          && broadcast.getAlreadyTriedDelegates().contains(delegateId)) {
        return abort(message);
      }

      if (!delegateService.filter(broadcast.getAccountId(), delegateId)) {
        return abort(message);
      }

      return new BroadcastAction(JsonUtils.asJson(aDelegateTaskEvent()
                                                      .withDelegateTaskId(broadcast.getTaskId())
                                                      .withSync(!broadcast.isAsync())
                                                      .withAccountId(broadcast.getAccountId())
                                                      .build()));
    }

    if (message instanceof DelegateTaskAbortEvent) {
      DelegateTaskAbortEvent abortEvent = (DelegateTaskAbortEvent) message;
      if (!delegateTaskServiceClassic.filter(delegateId, abortEvent)) {
        return abort(message);
      }

      return continueWith(message);
    }

    if (message instanceof String && ((String) message).startsWith("[X]")) {
      String msg = (String) message;
      int seqIndex = msg.lastIndexOf("[TOKEN]");
      if (seqIndex != -1) {
        msg = msg.substring(3, seqIndex);
      } else {
        msg = msg.substring(3);
      }

      if (!delegateId.equals(msg)) {
        return abort(message);
      }
      return continueWith(message);
    }

    if (message instanceof PerpetualTaskBroadcastEvent) {
      PerpetualTaskBroadcastEvent taskBroadcastEvent = (PerpetualTaskBroadcastEvent) message;

      if (isNotBlank(taskBroadcastEvent.getBroadcastDelegateId())
          && !StringUtils.equals(taskBroadcastEvent.getBroadcastDelegateId(), delegateId)) {
        return abort(message);
      }
      return continueWith(message);
    }
    return continueWith(message);
  }

  @NotNull
  private BroadcastAction continueWith(Object message) {
    return new BroadcastAction(message);
  }

  @NotNull
  private BroadcastAction abort(Object message) {
    return new BroadcastAction(ACTION.ABORT, message);
  }
}
