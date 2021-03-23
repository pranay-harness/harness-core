package software.wings.service.impl;

import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;

import static com.google.common.collect.Sets.symmetricDifference;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.configuration.DeployMode;
import io.harness.exception.WingsException;

import software.wings.beans.Account;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class HarnessUserGroupServiceImpl implements HarnessUserGroupService {
  @Inject WingsPersistence wingsPersistence;
  @Inject private AuthService authService;
  @Inject private AccountService accountService;

  @Override
  public HarnessUserGroup save(HarnessUserGroup harnessUserGroup) {
    String uuid = wingsPersistence.save(harnessUserGroup);
    return wingsPersistence.get(HarnessUserGroup.class, uuid);
  }

  @Override
  public PageResponse<HarnessUserGroup> list(PageRequest<HarnessUserGroup> req) {
    return wingsPersistence.query(HarnessUserGroup.class, req);
  }

  @Override
  public List<Account> listAllowedSupportAccounts(Set<String> excludeAccountIds) {
    Set<String> notSupportedAccounts = accountService.getAccountsWithDisabledHarnessUserGroupAccess();

    notSupportedAccounts.addAll(excludeAccountIds);

    List<Account> supportedAccounts = accountService.listAccounts(notSupportedAccounts);
    supportedAccounts.sort(new AccountComparator());
    return supportedAccounts;
  }

  private static class AccountComparator implements Comparator<Account>, Serializable {
    @Override
    public int compare(Account lhs, Account rhs) {
      return lhs.getAccountName().compareToIgnoreCase(rhs.getAccountName());
    }
  }

  @Override
  public HarnessUserGroup get(String uuid) {
    return wingsPersistence.get(HarnessUserGroup.class, uuid);
  }

  @Override
  public HarnessUserGroup updateMembers(String uuid, Set<String> memberIds) {
    HarnessUserGroup harnessUserGroup = get(uuid);

    if (harnessUserGroup == null) {
      throw new WingsException("No Harness user group found");
    }

    Set<String> oldMemberIds = harnessUserGroup.getMemberIds();

    UpdateOperations<HarnessUserGroup> updateOperations =
        wingsPersistence.createUpdateOperations(HarnessUserGroup.class);
    setUnset(updateOperations, "memberIds", memberIds);
    wingsPersistence.update(wingsPersistence.createQuery(HarnessUserGroup.class).filter("_id", uuid), updateOperations);
    HarnessUserGroup updatedUserGroup = get(uuid);

    SetView<String> membersAffected = symmetricDifference(updatedUserGroup.getMemberIds(), oldMemberIds);

    Set<String> accountsAffected =
        listAllowedSupportAccounts(Collections.emptySet()).stream().map(Account::getUuid).collect(Collectors.toSet());

    authService.evictUserPermissionAndRestrictionCacheForAccounts(
        accountsAffected, Lists.newArrayList(membersAffected));
    return updatedUserGroup;
  }

  @Override
  public boolean isHarnessSupportEnabledForAccount(String accountId) {
    Account account = accountService.get(accountId);
    return account.isHarnessSupportAccessAllowed();
  }

  @Override
  public boolean isHarnessSupportUser(String userId) {
    String deployMode = System.getenv(DeployMode.DEPLOY_MODE);
    if (DeployMode.isOnPrem(deployMode)) {
      return false;
    }

    Query<HarnessUserGroup> query = wingsPersistence.createQuery(HarnessUserGroup.class, excludeAuthority);
    query.filter("memberIds", userId);
    Key<HarnessUserGroup> userGroupKey = query.getKey();
    return userGroupKey != null;
  }

  @Override
  public boolean delete(String uuid) {
    return wingsPersistence.delete(HarnessUserGroup.class, uuid);
  }
}
