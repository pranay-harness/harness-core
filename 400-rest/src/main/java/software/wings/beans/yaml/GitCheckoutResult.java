/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
