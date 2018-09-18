package software.wings.app;

import static software.wings.app.DelegateStreamHandler.SPLITTER;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;

import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.BroadcastFilterAdapter;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.JsonUtils;

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

      if (versionMatched && preassignedIdMatched && delegateService.filter(delegateId, task)) {
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
    }
    return new BroadcastAction(message);
  }
}
