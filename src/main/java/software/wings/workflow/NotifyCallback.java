package software.wings.workflow;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public interface NotifyCallback extends Serializable {
  public void notify(Map<String, ? extends Serializable> response);
}
