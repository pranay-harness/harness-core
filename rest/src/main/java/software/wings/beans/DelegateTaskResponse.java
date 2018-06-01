package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateTaskResponse {
  private String accountId;
  private NotifyResponseData response;

  public DelegateTaskResponse() {}

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
    return Objects.hash(accountId, response);
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
    return Objects.equals(this.accountId, other.accountId) && Objects.equals(this.response, other.response);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("accountId", accountId).add("response", response).toString();
  }

  public static final class Builder {
    private String accountId;
    private NotifyResponseData response;

    private Builder() {}

    public static Builder aDelegateTaskResponse() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withResponse(NotifyResponseData response) {
      this.response = response;
      return this;
    }

    public Builder but() {
      return aDelegateTaskResponse().withAccountId(accountId).withResponse(response);
    }

    public DelegateTaskResponse build() {
      DelegateTaskResponse delegateTaskResponse = new DelegateTaskResponse();
      delegateTaskResponse.setAccountId(accountId);
      delegateTaskResponse.setResponse(response);
      return delegateTaskResponse;
    }
  }
}
