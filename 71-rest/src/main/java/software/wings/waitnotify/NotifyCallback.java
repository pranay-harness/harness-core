package software.wings.waitnotify;

import io.harness.task.protocol.ResponseData;

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
  void notify(Map<String, ResponseData> response);

  void notifyError(Map<String, ResponseData> response);
}
