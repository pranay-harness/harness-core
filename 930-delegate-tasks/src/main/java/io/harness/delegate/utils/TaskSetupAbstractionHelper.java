/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(PL)
public class TaskSetupAbstractionHelper {
  private static final String PROJECT_OWNER = "%s/%s";
  private static final String ORG_OWNER = "%s";

  public String getOwner(String accountId, String orgIdentifier, String projectIdentifier) {
    String owner = null;
    if (isNotEmpty(orgIdentifier) && isNotEmpty(projectIdentifier)) {
      owner = String.format(PROJECT_OWNER, orgIdentifier, projectIdentifier);
    } else if (isNotEmpty(orgIdentifier)) {
      owner = String.format(ORG_OWNER, orgIdentifier);
    }
    return owner;
  }
}
