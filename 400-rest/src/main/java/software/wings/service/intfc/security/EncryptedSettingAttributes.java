package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.Collection;
import java.util.Set;

@OwnedBy(CDC)
public interface EncryptedSettingAttributes extends OwnedByAccount {
  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId);

  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId, Set<String> categories);
}
