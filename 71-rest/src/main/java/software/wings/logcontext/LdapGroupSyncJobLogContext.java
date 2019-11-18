package software.wings.logcontext;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class LdapGroupSyncJobLogContext extends AutoLogContext {
  public LdapGroupSyncJobLogContext(String accountId, String ldapConfigId, OverrideBehavior behavior) {
    super(ImmutableMap.of("accountId", accountId, "ldapConfigId", ldapConfigId), behavior);
  }
}
