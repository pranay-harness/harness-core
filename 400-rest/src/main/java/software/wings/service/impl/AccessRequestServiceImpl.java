package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidAccessRequestException;
import io.harness.exception.InvalidArgumentsException;

import software.wings.beans.Account;
import software.wings.beans.Event.Type;
import software.wings.beans.User;
import software.wings.beans.security.AccessRequest;
import software.wings.beans.security.AccessRequestDTO;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccessRequestService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(_970_RBAC_CORE)
public class AccessRequestServiceImpl implements AccessRequestService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private UserService userService;
  @Inject private AuditServiceHelper auditServiceHelper;

  @Override
  public AccessRequest createAccessRequest(AccessRequestDTO accessRequestDTO) {
    Account account = accountService.get(accessRequestDTO.getAccountId());
    notNullCheck("Invalid account with id: " + accessRequestDTO.getAccountId(), account);

    if (account.isHarnessSupportAccessAllowed()) {
      throw new InvalidAccessRequestException(
          String.format("accountId: %s is not a restricted account and doesn't not require Access Request",
              accessRequestDTO.getAccountId()));
    }

    if (accessRequestDTO.getHours() != null) {
      accessRequestDTO.setAccessStartAt(Instant.now().toEpochMilli());
      accessRequestDTO.setAccessEndAt(Instant.now().plus(accessRequestDTO.getHours(), ChronoUnit.HOURS).toEpochMilli());
    } else if (accessRequestDTO.getAccessStartAt() >= accessRequestDTO.getAccessEndAt()) {
      throw new InvalidArgumentsException("Access Start Time needs to be before Access End Time");
    }

    if (isNotEmpty(accessRequestDTO.getEmailIds()) && isNotEmpty(accessRequestDTO.getHarnessUserGroupId())) {
      throw new InvalidArgumentsException(String.format("AccessRequest contains harnessUserGroupId: %s, emailIds: %s. "
              + "AccessRequest is either for HarnessUserGroup or for specific individual members, but not for both.",
          accessRequestDTO.getHarnessUserGroupId(), accessRequestDTO.getEmailIds()));
    }

    AccessRequest accessRequest = AccessRequest.builder()
                                      .accountId(accessRequestDTO.getAccountId())
                                      .accessStartAt(accessRequestDTO.getAccessStartAt())
                                      .accessEndAt(accessRequestDTO.getAccessEndAt())
                                      .accessActive(true)
                                      .build();
    if (isEmpty(accessRequestDTO.getEmailIds()) && isNotEmpty(accessRequestDTO.getHarnessUserGroupId())) {
      accessRequest.setHarnessUserGroupId(accessRequestDTO.getHarnessUserGroupId());
      accessRequest.setAccessType(AccessRequest.AccessType.GROUP_ACCESS);
    } else {
      accessRequest.setMemberIds(getMemberIds(accessRequestDTO));
      accessRequest.setAccessType(AccessRequest.AccessType.MEMBER_ACCESS);
    }
    String uuid = wingsPersistence.save(accessRequest);
    auditServiceHelper.reportForAuditingUsingAccountId(accessRequest.getAccountId(), null, accessRequest, Type.CREATE);
    return wingsPersistence.get(AccessRequest.class, uuid);
  }

  @Override
  public AccessRequest get(String accessRequestId) {
    return wingsPersistence.get(AccessRequest.class, accessRequestId);
  }

  @Override
  public List<AccessRequest> getActiveAccessRequest(String harnessUserGroupId) {
    HarnessUserGroup harnessUserGroup = harnessUserGroupService.get(harnessUserGroupId);
    notNullCheck(String.format("Invalid Harness User Group with id: %s", harnessUserGroupId), harnessUserGroup);
    Query<AccessRequest> query = wingsPersistence.createQuery(AccessRequest.class, excludeAuthority);
    query.filter("harnessUserGroupId", harnessUserGroupId);
    query.filter("accessActive", true);
    query.filter("accessType", AccessRequest.AccessType.GROUP_ACCESS);
    return query.asList();
  }

  @Override
  public List<AccessRequest> getActiveAccessRequestForAccount(String accountId) {
    Account account = accountService.get(accountId);
    notNullCheck("Invalid account with id: " + accountId, account);
    Query<AccessRequest> query = wingsPersistence.createQuery(AccessRequest.class, excludeAuthority);
    query.filter("accountId", accountId);
    query.filter("accessActive", true);
    return query.asList();
  }

  @Override
  public List<AccessRequest> getActiveAccessRequestForAccountAndUser(String accountId, String userId) {
    Account account = accountService.get(accountId);
    notNullCheck("Invalid account with id: " + accountId, account);
    User user = userService.get(userId);
    notNullCheck("Invalid account with id: " + userId, user);
    Query<AccessRequest> query = wingsPersistence.createQuery(AccessRequest.class, excludeAuthority);
    query.filter("accountId", accountId);
    query.filter("memberIds", userId);
    query.filter("accessActive", true);
    query.filter("accessType", AccessRequest.AccessType.MEMBER_ACCESS);
    return query.asList();
  }

  @Override
  public boolean delete(String accessRequestId) {
    AccessRequest accessRequest = get(accessRequestId);
    notNullCheck(String.format("Access Request with id: %s is not present", accessRequestId), accessRequest);
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(accessRequest.getAccountId(), accessRequest);
    return wingsPersistence.delete(AccessRequest.class, accessRequestId);
  }

  private void updateStatusAccessRequest(AccessRequest accessRequest, boolean acccessStatus) {
    UpdateOperations<AccessRequest> updateOperations = wingsPersistence.createUpdateOperations(AccessRequest.class);
    updateOperations.set("accessActive", acccessStatus);
    wingsPersistence.update(accessRequest, updateOperations);
  }

  @Override
  public void checkAndUpdateAccessRequests(AccessRequest accessRequest) {
    if (Instant.ofEpochMilli(accessRequest.getAccessEndAt()).isBefore(Instant.now())) {
      updateStatusAccessRequest(accessRequest, false);
      log.info("AccessRequest with id: {} has expired. accessActive updated to false", accessRequest.getUuid());
    }
  }

  private Set<String> getMemberIds(AccessRequestDTO accessRequestDTO) {
    if (isEmpty(accessRequestDTO.getEmailIds())) {
      throw new InvalidArgumentsException("No Email Ids specified");
    }

    // this is for the case of workflow, with multiple emailIds.
    if (accessRequestDTO.getEmailIds().size() == 1) {
      String emailIds = (String) accessRequestDTO.getEmailIds().toArray()[0];
      if (emailIds.contains(",")) {
        List<String> tokenizedEmailIds = tokenizeInput(emailIds);
        accessRequestDTO.setEmailIds(newHashSet(tokenizedEmailIds));
      }
    }

    Set<String> memberIds = new HashSet<>();
    accessRequestDTO.getEmailIds().forEach(emailId -> {
      User user = userService.getUserByEmail(emailId);
      notNullCheck(String.format("No User present with emailId : %s", emailId), user);
      memberIds.add(userService.getUserByEmail(emailId).getUuid());
    });
    return memberIds;
  }

  private List<String> tokenizeInput(String emailIds) {
    List<String> output = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(emailIds, ",");
    while (tokenizer.hasMoreElements()) {
      output.add(tokenizer.nextToken().replaceAll(" ", ""));
    }
    return output;
  }
}
