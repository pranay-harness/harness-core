package software.wings.beans;

import software.wings.waitnotify.NotifyResponseData;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
public class DelegateTaskResponse {
  private String accountId;
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
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, taskId, response);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DelegateTaskResponse other = (DelegateTaskResponse) obj;
    return Objects.equals(this.accountId, other.accountId) && Objects.equals(this.taskId, other.taskId)
        && Objects.equals(this.response, other.response);
  }

  public static final class Builder {
    private String accountId;
    private String taskId;
    private NotifyResponseData response;

    private Builder() {}

    public static Builder aDelegateTaskResponse() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withTaskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    public Builder withResponse(NotifyResponseData response) {
      this.response = response;
      return this;
    }

    public Builder but() {
      return aDelegateTaskResponse().withAccountId(accountId).withTaskId(taskId).withResponse(response);
    }

    public DelegateTaskResponse build() {
      DelegateTaskResponse delegateTaskResponse = new DelegateTaskResponse();
      delegateTaskResponse.setAccountId(accountId);
      delegateTaskResponse.setTaskId(taskId);
      delegateTaskResponse.setResponse(response);
      return delegateTaskResponse;
    }
  }
}
