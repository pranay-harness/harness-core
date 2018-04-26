package software.wings.app;

import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;

import com.google.inject.Inject;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.BroadcastFilterAdapter;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.JsonUtils;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class DelegateEventFilter extends BroadcastFilterAdapter {
  @Inject private DelegateService delegateService;

  @Override
  public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
    if (message instanceof DelegateTask) {
      String delegateId = r.getRequest().getParameter("delegateId");
      DelegateTask task = (DelegateTask) message;
      if (delegateService.filter(delegateId, task)) {
        return new BroadcastAction(JsonUtils.asJson(aDelegateTaskEvent()
                                                        .withDelegateTaskId(task.getUuid())
                                                        .withSync(!task.isAsync())
                                                        .withAccountId(task.getAccountId())
                                                        .build()));
      } else {
        return new BroadcastAction(ACTION.ABORT, message);
      }
    } else if (message instanceof DelegateTaskAbortEvent) {
      String delegateId = r.getRequest().getParameter("delegateId");
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
