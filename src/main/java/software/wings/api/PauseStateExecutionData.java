package software.wings.api;

import com.google.common.base.MoreObjects;

import software.wings.sm.ExecutionStatus;

/**
 * Created by peeyushaggarwal on 6/7/16.
 */
public class PauseStateExecutionData extends EmailStateExecutionData {
  private static final long serialVersionUID = 7716476856171051155L;

  private String resumeId;

  public String getResumeId() {
    return resumeId;
  }

  public void setResumeId(String resumeId) {
    this.resumeId = resumeId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("resumeId", resumeId).toString();
  }

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

    public static Builder aPauseStateExecutionData() {
      return new Builder();
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withResumeId(String resumeId) {
      this.resumeId = resumeId;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
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
