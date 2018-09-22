package software.wings.waitnotify;

import java.util.Map;

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

  void notifyError(Map<String, NotifyResponseData> response);
}
