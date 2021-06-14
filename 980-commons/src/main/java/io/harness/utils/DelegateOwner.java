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
  public static Map<String, String> getNGTaskSetupAbstractionsWithOwner(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("ng", "true");

    String owner = null;
    if (isNotEmpty(orgIdentifier)) {
      owner = orgIdentifier;
      if (isNotEmpty(projectIdentifier)) {
        owner = orgIdentifier + "/" + projectIdentifier;
      }
    }

    if (isNotEmpty(owner)) {
      setupAbstractions.put("owner", owner);
    }

    return setupAbstractions;
  }
}
