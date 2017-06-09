package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.glassfish.jersey.jaxb.internal.XmlJaxbElementProvider;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Objects;

/**
 * Created by sgurubelli on 6/7/17.
 */
public class ApprovalDetails {
  @NotEmpty private String approvalId;
  private EmbeddedUser approvedBy;
  private String comments;

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

  public static final class Builder {
    private String approvalId;
    private EmbeddedUser approvedBy;
    private String comments;

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
    public Builder but() {
      return anApprovalDetails().withAnApprovalId(approvalId).withApprovedBy(approvedBy).withComments(comments);
    }

    public ApprovalDetails build() {
      ApprovalDetails approvalDetails = new ApprovalDetails();
      approvalDetails.setApprovalId(approvalId);
      approvalDetails.setApprovedBy(approvedBy);
      approvalDetails.setComments(comments);
      return approvalDetails;
    }
  }
}
