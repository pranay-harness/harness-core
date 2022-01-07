/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 12/19/2017
 */
@Data
@Builder
public class GitSyncErrorAlert implements AlertData {
  private String accountId;
  private String message;
  private boolean gitToHarness;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((GitSyncErrorAlert) alertData).accountId);
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
