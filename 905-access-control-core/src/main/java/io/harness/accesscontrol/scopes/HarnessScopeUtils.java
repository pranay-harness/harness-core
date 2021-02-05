package io.harness.accesscontrol.scopes;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HarnessScopeUtils {
  public static Map<String, String> getIdentifierMap(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Map<String, String> identifiers = new HashMap<>();
    identifiers.put(HarnessScope.ACCOUNT.getIdentifierKey(), accountIdentifier);
    identifiers.put(HarnessScope.ORGANIZATION.getIdentifierKey(), orgIdentifier);
    identifiers.put(HarnessScope.PROJECT.getIdentifierKey(), projectIdentifier);
    return identifiers;
  }
}
