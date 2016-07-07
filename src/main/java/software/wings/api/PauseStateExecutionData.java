package software.wings.api;

import com.google.common.base.MoreObjects;

import software.wings.sm.ExecutionStatus;

/**
 * Created by peeyushaggarwal on 6/7/16.
 */
public class PauseStateExecutionData extends EmailStateExecutionData {
  private String resumeId;

  /**
   * Gets resume id.
   *
   * @return the resume id
   */
  public String getResumeId() {
    return resumeId;
  }

  /**
   * Sets resume id.
   *
   * @param resumeId the resume id
   */
  public void setResumeId(String resumeId) {
    this.resumeId = resumeId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("resumeId", resumeId).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String resumeId;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String toAddress;
    private String ccAddress;
    private String subject;
    private String body;

    private Builder() {}

    /**
     * A pause state execution data builder.
     *
     * @return the builder
     */
    public static Builder aPauseStateExecutionData() {
      return new Builder();
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With resume id builder.
     *
     * @param resumeId the resume id
     * @return the builder
     */
    public Builder withResumeId(String resumeId) {
      this.resumeId = resumeId;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With to address builder.
     *
     * @param toAddress the to address
     * @return the builder
     */
    public Builder withToAddress(String toAddress) {
      this.toAddress = toAddress;
      return this;
    }

    /**
     * With cc address builder.
     *
     * @param ccAddress the cc address
     * @return the builder
     */
    public Builder withCcAddress(String ccAddress) {
      this.ccAddress = ccAddress;
      return this;
    }

    /**
     * With subject builder.
     *
     * @param subject the subject
     * @return the builder
     */
    public Builder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    /**
     * With body builder.
     *
     * @param body the body
     * @return the builder
     */
    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPauseStateExecutionData()
          .withStateName(stateName)
          .withResumeId(resumeId)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withToAddress(toAddress)
          .withCcAddress(ccAddress)
          .withSubject(subject)
          .withBody(body);
    }

    /**
     * Build pause state execution data.
     *
     * @return the pause state execution data
     */
    public PauseStateExecutionData build() {
      PauseStateExecutionData pauseStateExecutionData = new PauseStateExecutionData();
      pauseStateExecutionData.setStateName(stateName);
      pauseStateExecutionData.setResumeId(resumeId);
      pauseStateExecutionData.setStartTs(startTs);
      pauseStateExecutionData.setEndTs(endTs);
      pauseStateExecutionData.setStatus(status);
      pauseStateExecutionData.setToAddress(toAddress);
      pauseStateExecutionData.setCcAddress(ccAddress);
      pauseStateExecutionData.setSubject(subject);
      pauseStateExecutionData.setBody(body);
      return pauseStateExecutionData;
    }
  }
}
