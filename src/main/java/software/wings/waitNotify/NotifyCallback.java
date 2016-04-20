package software.wings.waitNotify;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Rishi
 */
public interface NotifyCallback extends Serializable { void notify(Map<String, ? extends Serializable> response); }
