package io.harness.delegate.beans.git;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCheckoutResult extends GitCommandResult {
  private String refName;
  private String objectId;

  /**
   * Instantiates a new Git checkout result.
   */
  public GitCheckoutResult() {
    super(GitCommandType.CHECKOUT);
  }

  /**
   * Instantiates a new Git checkout result.
   *
   * @param refName  the ref name
   * @param objectId the object id
   */
  public GitCheckoutResult(String refName, String objectId) {
    super(GitCommandType.CHECKOUT);
    this.refName = refName;
    this.objectId = objectId;
  }
}
