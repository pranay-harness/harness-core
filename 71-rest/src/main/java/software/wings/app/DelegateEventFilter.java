package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.app.DelegateStreamHandler.SPLITTER;
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
import software.wings.beans.FeatureName;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class DelegateEventFilter extends BroadcastFilterAdapter {
  @Inject private DelegateService delegateService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
    AtmosphereRequest req = r.getRequest();
    List<String> pathSegments = SPLITTER.splitToList(req.getPathInfo());
    String accountId = pathSegments.get(1);
    String delegateId = req.getParameter("delegateId");
    String version = req.getHeader("Version");

    if (message instanceof DelegateTask) {
      DelegateTask task = (DelegateTask) message;

      boolean versionMatched = true;

      if (featureFlagService.isEnabled(FeatureName.DELEGATE_TASK_VERSIONING, accountId)) {
        if (!StringUtils.equals(version, task.getVersion())) {
          versionMatched = false;
        }
      }

      boolean preassignedIdMatched = true;
      if (StringUtils.isNotEmpty(task.getPreAssignedDelegateId())
          && !StringUtils.equals(task.getPreAssignedDelegateId(), delegateId)) {
        preassignedIdMatched = false;
      }

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
