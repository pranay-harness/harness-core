package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.serializer.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.BroadcastFilterAdapter;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.service.intfc.DelegateService;

public class DelegateEventFilter extends BroadcastFilterAdapter {
  @Inject private DelegateService delegateService;

  @Override
  public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
    AtmosphereRequest req = r.getRequest();
    String delegateId = req.getParameter("delegateId");
    String version = req.getHeader("Version");

    if (message instanceof DelegateTask) {
      DelegateTask task = (DelegateTask) message;

      boolean versionMatched = StringUtils.equals(version, task.getVersion());

      boolean preassignedIdMatched =
          isBlank(task.getPreAssignedDelegateId()) || StringUtils.equals(task.getPreAssignedDelegateId(), delegateId);

      boolean delegateAlreadyTried =
          isNotEmpty(task.getAlreadyTriedDelegates()) && task.getAlreadyTriedDelegates().contains(delegateId);

      if (versionMatched && preassignedIdMatched && !delegateAlreadyTried && delegateService.filter(delegateId, task)) {
        return new BroadcastAction(JsonUtils.asJson(aDelegateTaskEvent()
                                                        .withDelegateTaskId(task.getUuid())
                                                        .withSync(!task.isAsync())
                                                        .withAccountId(task.getAccountId())
                                                        .build()));
      } else {
        return new BroadcastAction(ACTION.ABORT, message);
      }
    } else if (message instanceof DelegateTaskAbortEvent) {
      DelegateTaskAbortEvent abortEvent = (DelegateTaskAbortEvent) message;
      if (delegateService.filter(delegateId, abortEvent)) {
        return new BroadcastAction(message);
      } else {
        return new BroadcastAction(ACTION.ABORT, message);
      }
    } else if (message instanceof String && ((String) message).startsWith("[X]")) {
      String msg = (String) message;
      int seqIndex = msg.lastIndexOf("[TOKEN]");
      if (seqIndex != -1) {
        msg = msg.substring(3, seqIndex);
      } else {
        msg = msg.substring(3);
      }

      if (delegateId.equals(msg)) {
        return new BroadcastAction(message);
      } else {
        return new BroadcastAction(ACTION.ABORT, message);
      }
    }
    return new BroadcastAction(message);
  }
}
