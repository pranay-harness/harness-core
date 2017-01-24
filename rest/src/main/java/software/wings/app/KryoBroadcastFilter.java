package software.wings.app;

import org.atmosphere.cpr.BroadcastFilter;
import software.wings.beans.DelegateTask;
import software.wings.utils.KryoUtils;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class KryoBroadcastFilter implements BroadcastFilter {
  @Override
  public BroadcastAction filter(String broadcasterId, Object originalMessage, Object message) {
    if (message.getClass() == DelegateTask.class) {
      return new BroadcastAction(KryoUtils.asString(message));
    }
    return new BroadcastAction(message);
  }
}
