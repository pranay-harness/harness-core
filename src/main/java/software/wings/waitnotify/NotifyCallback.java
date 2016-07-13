package software.wings.waitnotify;

import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Function to call when all correlationIds are completed for a wait instance.
 *
 * @author Rishi
 */
public interface NotifyCallback {
  /**
   * Notify.
   *
   * @param response the response
   */
  void notify(Map<String, NotifyResponseData> response);
}
