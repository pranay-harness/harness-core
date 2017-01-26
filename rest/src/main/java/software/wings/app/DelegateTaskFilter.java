package software.wings.app;

import static software.wings.beans.DelegateTaskEvent.Builder.aDelegateTaskEvent;

import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.BroadcastFilterAdapter;
import software.wings.beans.DelegateTask;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.JsonUtils;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class DelegateTaskFilter extends BroadcastFilterAdapter {
  @Inject private DelegateService delegateService;

  @Override
  public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
    if (message.getClass() == DelegateTask.class) {
      String delegateId = r.getRequest().getParameter("delegateId");
      DelegateTask task = (DelegateTask) message;
      if (delegateService.filter(delegateId, task)) {
        return new BroadcastAction(JsonUtils.asJson(aDelegateTaskEvent()
                                                        .withDelegateTaskId(task.getUuid())
                                                        .withSync(StringUtils.isNotBlank(task.getQueueName()))
                                                        .withAccountId(task.getAccountId())
                                                        .build()));
      } else {
        return new BroadcastAction(ACTION.ABORT, message);
      }
    }
    return new BroadcastAction(message);
  }
}
