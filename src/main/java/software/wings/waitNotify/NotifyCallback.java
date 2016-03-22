package software.wings.waitNotify;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public interface NotifyCallback extends Serializable { public void notify(Map<String, Serializable> response); }
