package software.wings.beans;

import io.harness.beans.EmbeddedUser;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 6/7/17.
 */
@Data
@NoArgsConstructor
public class ApprovalDetails {
  @NotEmpty private String approvalId;
  private EmbeddedUser approvedBy;
  private String comments;
  private Action action;
  private boolean approvalFromSlack;
  private List<NameValuePair> variables;

  public enum Action {
    /**
     * Approve action
     */
    APPROVE,
    /** Reject Action */
    REJECT
  }
}
