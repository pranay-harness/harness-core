package software.wings.api;

import software.wings.beans.User;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

/**
 * Created by anubhaw on 11/10/16.
 */
public class ApprovalStateExecutionData extends StateExecutionData {
  private User approvedBy;
  private Long approvedOn;
  private String comments;

  /**
   * Gets approved by.
   *
   * @return the approved by
   */
  public User getApprovedBy() {
    return approvedBy;
  }

  /**
   * Sets approved by.
   *
   * @param approvedBy the approved by
   */
  public void setApprovedBy(User approvedBy) {
    this.approvedBy = approvedBy;
  }

  /**
   * Gets approved on.
   *
   * @return the approved on
   */
  public Long getApprovedOn() {
    return approvedOn;
  }

  /**
   * Sets approved on.
   *
   * @param approvedOn the approved on
   */
  public void setApprovedOn(Long approvedOn) {
    this.approvedOn = approvedOn;
  }

  /**
   * Gets comments.
   *
   * @return the comments
   */
  public String getComments() {
    return comments;
  }

  /**
   * Sets comments.
   *
   * @param comments the comments
   */
  public void setComments(String comments) {
    this.comments = comments;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private User approvedBy;
    private Long approvedOn;
    private String comments;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    /**
     * An approval state execution data builder.
     *
     * @return the builder
     */
    public static Builder anApprovalStateExecutionData() {
      return new Builder();
    }

    /**
     * With approved by builder.
     *
     * @param approvedBy the approved by
     * @return the builder
     */
    public Builder withApprovedBy(User approvedBy) {
      this.approvedBy = approvedBy;
      return this;
    }

    /**
     * With approved on builder.
     *
     * @param approvedOn the approved on
     * @return the builder
     */
    public Builder withApprovedOn(Long approvedOn) {
      this.approvedOn = approvedOn;
      return this;
    }

    /**
     * With comments builder.
     *
     * @param comments the comments
     * @return the builder
     */
    public Builder withComments(String comments) {
      this.comments = comments;
      return this;
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
     * With error msg builder.
     *
     * @param errorMsg the error msg
     * @return the builder
     */
    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anApprovalStateExecutionData()
          .withApprovedBy(approvedBy)
          .withApprovedOn(approvedOn)
          .withComments(comments)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build approval state execution data.
     *
     * @return the approval state execution data
     */
    public ApprovalStateExecutionData build() {
      ApprovalStateExecutionData approvalStateExecutionData = new ApprovalStateExecutionData();
      approvalStateExecutionData.setApprovedBy(approvedBy);
      approvalStateExecutionData.setApprovedOn(approvedOn);
      approvalStateExecutionData.setComments(comments);
      approvalStateExecutionData.setStateName(stateName);
      approvalStateExecutionData.setStartTs(startTs);
      approvalStateExecutionData.setEndTs(endTs);
      approvalStateExecutionData.setStatus(status);
      approvalStateExecutionData.setErrorMsg(errorMsg);
      return approvalStateExecutionData;
    }
  }
}
