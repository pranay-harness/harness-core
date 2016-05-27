package software.wings.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
public class EmailStateExecutionData extends StateExecutionData {
  private static final long serialVersionUID = -8664130788122512084L;
  private String toAddress;
  private String ccAddress;
  private String subject;
  private String body;

  public String getToAddress() {
    return toAddress;
  }

  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  public String getCcAddress() {
    return ccAddress;
  }

  public void setCcAddress(String ccAddress) {
    this.ccAddress = ccAddress;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    EmailStateExecutionData that = (EmailStateExecutionData) o;
    return Objects.equal(toAddress, that.toAddress) && Objects.equal(ccAddress, that.ccAddress)
        && Objects.equal(subject, that.subject) && Objects.equal(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(toAddress, ccAddress, subject, body);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("toAddress", toAddress)
        .add("ccAddress", ccAddress)
        .add("subject", subject)
        .add("body", body)
        .toString();
  }

  public static final class Builder {
    private String toAddress;
    private String ccAddress;
    private String subject;
    private String body;
    private String stateName;
    private long startTs;
    private long endTs;
    private ExecutionStatus status;

    private Builder() {}

    public static Builder anEmailStateExecutionData() {
      return new Builder();
    }

    public Builder withToAddress(String toAddress) {
      this.toAddress = toAddress;
      return this;
    }

    public Builder withCcAddress(String ccAddress) {
      this.ccAddress = ccAddress;
      return this;
    }

    public Builder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStartTs(long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder but() {
      return anEmailStateExecutionData()
          .withToAddress(toAddress)
          .withCcAddress(ccAddress)
          .withSubject(subject)
          .withBody(body)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status);
    }

    public EmailStateExecutionData build() {
      EmailStateExecutionData emailStateExecutionData = new EmailStateExecutionData();
      emailStateExecutionData.setToAddress(toAddress);
      emailStateExecutionData.setCcAddress(ccAddress);
      emailStateExecutionData.setSubject(subject);
      emailStateExecutionData.setBody(body);
      emailStateExecutionData.setStateName(stateName);
      emailStateExecutionData.setStartTs(startTs);
      emailStateExecutionData.setEndTs(endTs);
      emailStateExecutionData.setStatus(status);
      return emailStateExecutionData;
    }
  }
}
