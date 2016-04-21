package software.wings.waitnotify;

import java.io.Serializable;
import java.util.Map;

/**
 * Function to call when all correlationIds are completed for a wait instance.
 * @author Rishi
 */
public interface NotifyCallback extends Serializable { void notify(Map<String, ? extends Serializable> response); }
