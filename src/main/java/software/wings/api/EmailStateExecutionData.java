package software.wings.api;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;
import static software.wings.common.Constants.SUMMARY_PAYLOAD_LIMIT;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
public class EmailStateExecutionData extends StateExecutionData {
  private String toAddress;
  private String ccAddress;
  private String subject;
  private String body;

  /**
   * Gets to address.
   *
   * @return the to address
   */
  public String getToAddress() {
    return toAddress;
  }

  /**
   * Sets to address.
   *
   * @param toAddress the to address
   */
  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  /**
   * Gets cc address.
   *
   * @return the cc address
   */
  public String getCcAddress() {
    return ccAddress;
  }

  /**
   * Sets cc address.
   *
   * @param ccAddress the cc address
   */
  public void setCcAddress(String ccAddress) {
    this.ccAddress = ccAddress;
  }

  /**
   * Gets subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets subject.
   *
   * @param subject the subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Gets body.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets body.
   *
   * @param body the body
   */
  public void setBody(String body) {
    this.body = body;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailStateExecutionData that = (EmailStateExecutionData) o;
    return Objects.equal(toAddress, that.toAddress) && Objects.equal(ccAddress, that.ccAddress)
        && Objects.equal(subject, that.subject) && Objects.equal(body, that.body);
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(
        executionDetails, "toAddress", anExecutionDataValue().withValue(toAddress).withDisplayName("To").build());
    putNotNull(
        executionDetails, "ccAddress", anExecutionDataValue().withValue(ccAddress).withDisplayName("CC").build());
    putNotNull(
        executionDetails, "subject", anExecutionDataValue().withValue(subject).withDisplayName("Subject").build());
    putNotNull(executionDetails, "body",
        anExecutionDataValue().withValue(abbreviate(body, SUMMARY_PAYLOAD_LIMIT)).withDisplayName("Body").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "toAddress", anExecutionDataValue().withValue(toAddress).withDisplayName("To").build());
    putNotNull(
        executionDetails, "ccAddress", anExecutionDataValue().withValue(ccAddress).withDisplayName("CC").build());
    putNotNull(
        executionDetails, "subject", anExecutionDataValue().withValue(subject).withDisplayName("Subject").build());
    putNotNull(executionDetails, "body", anExecutionDataValue().withValue(body).withDisplayName("Body").build());

    return executionDetails;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(toAddress, ccAddress, subject, body);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("toAddress", toAddress)
        .add("ccAddress", ccAddress)
        .add("subject", subject)
        .add("body", body)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String toAddress;
    private String ccAddress;
    private String subject;
    private String body;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;

    private Builder() {}

    /**
     * An email state execution data.
     *
     * @return the builder
     */
    public static Builder anEmailStateExecutionData() {
      return new Builder();
    }

    /**
     * With to address.
     *
     * @param toAddress the to address
     * @return the builder
     */
    public Builder withToAddress(String toAddress) {
      this.toAddress = toAddress;
      return this;
    }

    /**
     * With cc address.
     *
     * @param ccAddress the cc address
     * @return the builder
     */
    public Builder withCcAddress(String ccAddress) {
      this.ccAddress = ccAddress;
      return this;
    }

    /**
     * With subject.
     *
     * @param subject the subject
     * @return the builder
     */
    public Builder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    /**
     * With body.
     *
     * @param body the body
     * @return the builder
     */
    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    /**
     * With state name.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With start ts.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
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

    /**
     * Builds the.
     *
     * @return the email state execution data
     */
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
