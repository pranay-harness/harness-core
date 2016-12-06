package software.wings.beans;

import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
public class DelegateTaskResponse {
  private String appId;
  private String taskId;
  private NotifyResponseData response;

  /**
   * Getter for property 'taskId'.
   *
   * @return Value for property 'taskId'.
   */
  public String getTaskId() {
    return taskId;
  }

  /**
   * Setter for property 'taskId'.
   *
   * @param taskId Value to set for property 'taskId'.
   */
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  /**
   * Getter for property 'response'.
   *
   * @return Value for property 'response'.
   */
  public NotifyResponseData getResponse() {
    return response;
  }

  /**
   * Setter for property 'response'.
   *
   * @param response Value to set for property 'response'.
   */
  public void setResponse(NotifyResponseData response) {
    this.response = response;
  }

  /**
   * Getter for property 'appId'.
   *
   * @return Value for property 'appId'.
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Setter for property 'appId'.
   *
   * @param appId Value to set for property 'appId'.
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }
}
