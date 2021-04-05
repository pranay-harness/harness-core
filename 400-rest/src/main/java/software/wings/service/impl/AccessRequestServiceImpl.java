package software.wings.service.impl;

import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;

import static com.google.common.collect.Sets.symmetricDifference;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;

import software.wings.beans.Account;
import software.wings.beans.security.AccessRequest;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccessRequestService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessRequestServiceImpl implements AccessRequestService {
  @Inject WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private HarnessUserGroupService harnessUserGroupService;

  @Override
  public AccessRequest create(
      String accountId, String harnessUserGroupId, long accessStartAt, long accessEndAt, boolean accessActive) {
    AccessRequest accessRequest = AccessRequest.builder()
                                      .accountId(accountId)
                                      .harnessUserGroupId(harnessUserGroupId)
                                      .accessStartAt(accessStartAt)
                                      .accessEndAt(accessEndAt)
                                      .accessActive(accessActive)
                                      .build();
    String uuid = wingsPersistence.save(accessRequest);
    return wingsPersistence.get(AccessRequest.class, uuid);
  }

  @Override
  public AccessRequest get(String accessRequestId) {
    return wingsPersistence.get(AccessRequest.class, accessRequestId);
  }

  @Override
  public List<AccessRequest> getActiveAccessRequest(String harnessUserGroupId) {
    HarnessUserGroup harnessUserGroup = harnessUserGroupService.get(harnessUserGroupId);
    notNullCheck(String.format("Invalid Harness User Group with id: {}", harnessUserGroupId), harnessUserGroup);
    Query<AccessRequest> query = wingsPersistence.createQuery(AccessRequest.class, excludeAuthority);
    query.filter("harnessUserGroupId", harnessUserGroupId);
    query.filter("accessActive", true);
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
  public boolean update(String accessRequestId, long accessStartAt, long accessEndAt) {
    AccessRequest accessRequest = wingsPersistence.get(AccessRequest.class, accessRequestId);
    notNullCheck(String.format("Invalid Access Request with id: {}", accessRequestId), accessRequest);
    UpdateOperations<AccessRequest> updateOperations = wingsPersistence.createUpdateOperations(AccessRequest.class);
    updateOperations.set("accessStartAt", accessStartAt);
    updateOperations.set("accessEndAt", accessEndAt);
    wingsPersistence.update(accessRequest, updateOperations);
    return true;
  }

  @Override
  public boolean delete(String accessRequestId) {
    return wingsPersistence.delete(AccessRequest.class, accessRequestId);
  }

  @Override
  public boolean updateStatus(String accessRequestId, boolean acccessStatus) {
    AccessRequest accessRequest = get(accessRequestId);
    notNullCheck(String.format("Invalid Access Request with id: {}", accessRequestId), accessRequest);
    UpdateOperations<AccessRequest> updateOperations = wingsPersistence.createUpdateOperations(AccessRequest.class);
    updateOperations.set("accessActive", acccessStatus);
    wingsPersistence.update(accessRequest, updateOperations);
    return true;
  }
}
