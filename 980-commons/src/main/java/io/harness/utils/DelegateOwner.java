/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class DelegateOwner {
  public static final String NG_DELEGATE_ENABLED_CONSTANT = "ng";
  public static final String NG_DELEGATE_OWNER_CONSTANT = "owner";

  public static Map<String, String> getNGTaskSetupAbstractionsWithOwner(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(NG_DELEGATE_ENABLED_CONSTANT, "true");

    String owner = null;
    if (isNotEmpty(orgIdentifier)) {
      owner = orgIdentifier;
      if (isNotEmpty(projectIdentifier)) {
        owner = orgIdentifier + "/" + projectIdentifier;
      }
    }

    if (isNotEmpty(owner)) {
      setupAbstractions.put(NG_DELEGATE_OWNER_CONSTANT, owner);
    }

    return setupAbstractions;
  }
}
