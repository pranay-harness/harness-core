package software.wings.service.impl;

import static com.google.common.collect.Sets.symmetricDifference;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Singleton
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
  public Set<Action> listAllowedUserActionsForAccount(String accountId, String userId) {
    Query<HarnessUserGroup> query = wingsPersistence.createQuery(HarnessUserGroup.class, excludeAuthority);
    query.filter("memberIds", userId);
    query.or(query.criteria("applyToAllAccounts").equal(true), query.criteria("accountIds").equal(accountId));
    List<HarnessUserGroup> harnessUserGroups = query.asList();
    Set<Action> actionSet = Sets.newHashSet();
    harnessUserGroups.forEach(harnessUserGroup -> {
      Set<Action> actions = harnessUserGroup.getActions();
      if (isNotEmpty(actions)) {
        actionSet.addAll(actions);
      }
    });
    return actionSet;
  }

  @Override
  public List<Account> listAllowedSupportAccountsForUser(String userId, Set<String> excludeAccountIds) {
    Query<HarnessUserGroup> query = wingsPersistence.createQuery(HarnessUserGroup.class, excludeAuthority);
    query.filter("memberIds", userId);
    List<HarnessUserGroup> harnessUserGroups = query.asList();

    Set<String> accountIds = Sets.newHashSet();
    for (HarnessUserGroup harnessUserGroup : harnessUserGroups) {
      // If any of the groups have all accounts selected, we just return all the accounts except the excluded ones
      if (harnessUserGroup.isApplyToAllAccounts()) {
        if (isNotEmpty(excludeAccountIds)) {
          return accountService.list(
              aPageRequest().addFilter("_id", Operator.NOT_IN, excludeAccountIds.toArray()).build());
        } else {
          return accountService.list(aPageRequest().build());
        }
      } else {
        Set<String> accountIdsFromUserGroup = harnessUserGroup.getAccountIds();
        if (isNotEmpty(accountIdsFromUserGroup)) {
          accountIds.addAll(accountIdsFromUserGroup);
        }
      }
    }

    accountIds.removeAll(excludeAccountIds);
    if (isNotEmpty(accountIds)) {
      List<Account> accountList =
          accountService.list(aPageRequest().addFilter("_id", Operator.IN, accountIds.toArray()).build());
      accountList.sort(new AccountComparator());
      return accountList;
    }

    return Collections.emptyList();
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
  public HarnessUserGroup updateAccounts(String uuid, Set<String> accountIds) {
    HarnessUserGroup harnessUserGroup = get(uuid);

    if (harnessUserGroup == null) {
      throw new WingsException("No Harness user group found");
    }

    boolean applyToAllAccounts = harnessUserGroup.isApplyToAllAccounts();
    Set<String> oldAccountIds;
    if (applyToAllAccounts) {
      List<String> oldAccountIdsList = accountService.list(aPageRequest().withLimit("10000").build());
      oldAccountIds = Sets.newHashSet(oldAccountIdsList);
    } else {
      oldAccountIds = harnessUserGroup.getAccountIds();
    }

    UpdateOperations<HarnessUserGroup> updateOperations =
        wingsPersistence.createUpdateOperations(HarnessUserGroup.class);
    setUnset(updateOperations, "accountIds", accountIds);
    updateOperations.set("applyToAllAccounts", false);
    wingsPersistence.update(wingsPersistence.createQuery(HarnessUserGroup.class).filter("_id", uuid), updateOperations);
    HarnessUserGroup updatedUserGroup = get(uuid);

    SetView<String> accountsAffected = symmetricDifference(updatedUserGroup.getAccountIds(), oldAccountIds);
    authService.evictAccountUserPermissionInfoCache(
        accountsAffected, Lists.newArrayList(updatedUserGroup.getMemberIds()));
    return updatedUserGroup;
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

    boolean applyToAllAccounts = updatedUserGroup.isApplyToAllAccounts();
    Set<String> accountsAffected;
    if (applyToAllAccounts) {
      List<String> oldAccountIdsList = accountService.list(aPageRequest().withLimit("10000").build());
      accountsAffected = Sets.newHashSet(oldAccountIdsList);
    } else {
      accountsAffected = updatedUserGroup.getAccountIds();
    }

    authService.evictAccountUserPermissionInfoCache(accountsAffected, Lists.newArrayList(membersAffected));
    return updatedUserGroup;
  }

  @Override
  public boolean isHarnessSupportUser(String userId) {
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
