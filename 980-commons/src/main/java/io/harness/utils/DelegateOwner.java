/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
