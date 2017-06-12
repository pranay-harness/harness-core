package software.wings.beans;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.Objects;

/**
 * Created by sgurubelli on 6/7/17.
 */
public class ApprovalDetails {
  @NotEmpty private String approvalId;
  private EmbeddedUser approvedBy;
  private String comments;
  private Action action;

  /**
   * Get ApprovalId
   * @return approvalId
   */
  public String getApprovalId() {
    return approvalId;
  }

  /**
   * Set ApprovalId
   * @param approvalId
   */
  public void setApprovalId(String approvalId) {
    this.approvalId = approvalId;
  }

  /**
   * Get Approved By
   * @return
   */
  public EmbeddedUser getApprovedBy() {
    return approvedBy;
  }

  /**
   * Set approved By
   * @param approvedBy
   */
  public void setApprovedBy(EmbeddedUser approvedBy) {
    this.approvedBy = approvedBy;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  /**
   * Get Action.
   * @return
   */
  public Action getAction() {
    return action;
  }

  /**
   * Set Action.
   * @param action
   */
  public void setAction(Action action) {
    this.action = action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ApprovalDetails that = (ApprovalDetails) o;
    return Objects.equals(approvalId, that.approvalId) && Objects.equals(approvedBy, that.approvedBy)
        && Objects.equals(comments, that.comments) && action == that.action;
  }

  @Override
  public int hashCode() {
    return Objects.hash(approvalId, approvedBy, comments, action);
  }

  public enum Action {
    /**
     * Approve action
     */
    APPROVE, /** Reject Action */
    REJECT
  }
  public static final class Builder {
    private String approvalId;
    private EmbeddedUser approvedBy;
    private String comments;
    private Action action;

    private Builder() {}

    public static Builder anApprovalDetails() {
      return new Builder();
    }

    public Builder withAnApprovalId(String approvalId) {
      this.approvalId = approvalId;
      return this;
    }

    public Builder withApprovedBy(EmbeddedUser user) {
      this.approvedBy = approvedBy;
      return this;
    }

    public Builder withComments(String comments) {
      this.comments = comments;
      return this;
    }

    public Builder withAction(Action action) {
      this.action = action;
      return this;
    }
    public Builder but() {
      return anApprovalDetails()
          .withAnApprovalId(approvalId)
          .withApprovedBy(approvedBy)
          .withComments(comments)
          .withAction(action);
    }

    public ApprovalDetails build() {
      ApprovalDetails approvalDetails = new ApprovalDetails();
      approvalDetails.setApprovalId(approvalId);
      approvalDetails.setApprovedBy(approvedBy);
      approvalDetails.setComments(comments);
      approvalDetails.setAction(action);
      return approvalDetails;
    }
  }
}
