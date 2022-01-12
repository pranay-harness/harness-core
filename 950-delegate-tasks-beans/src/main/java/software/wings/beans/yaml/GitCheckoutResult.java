/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 10/16/17.
 */
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
