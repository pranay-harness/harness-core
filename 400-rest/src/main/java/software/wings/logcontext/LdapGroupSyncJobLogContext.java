package software.wings.logcontext;

import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;

public class LdapGroupSyncJobLogContext extends AutoLogContext {
  public LdapGroupSyncJobLogContext(String accountId, String ldapConfigId, OverrideBehavior behavior) {
    super(ImmutableMap.of("accountId", accountId, "ldapConfigId", ldapConfigId), behavior);
  }
}
