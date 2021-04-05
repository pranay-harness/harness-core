package software.wings.service.impl;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Account;
import software.wings.beans.security.AccessRequest;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccessRequestService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.util.List;
import org.mongodb.morphia.query.Query;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessRequestServiceImpl implements AccessRequestService {
  @Inject WingsPersistence wingsPersistence;
  @Inject AccountService accountService;
  @Inject HarnessUserGroupService harnessUserGroupService;

//  @Override
//  public AccessRequest create(
//      String accountId, String harnessUserGroupId, long accessStartAt, long accessEndAt, boolean accessActive) {
//    AccessRequest accessRequest = AccessRequest.builder()
//                                      .accountId(accountId)
//                                      .harnessUserGroupId(harnessUserGroupId)
//                                      .accessStartAt(accessStartAt)
//                                      .accessEndAt(accessEndAt)
//                                      .accessActive(accessActive)
//                                      .build();
//    String uuid = wingsPersistence.save(accessRequest);
//    return wingsPersistence.get(AccessRequest.class, uuid);
//  }
//
//  @Override
//  public AccessRequest get(String accessRequestId) {
//    return wingsPersistence.get(AccessRequest.class, accessRequestId);
//  }
//
//  @Override
//  public List<AccessRequest> getActiveAccessRequest(String harnessUserGroupId) {
//    HarnessUserGroup harnessUserGroup = harnessUserGroupService.get(harnessUserGroupId);
//    notNullCheck(String.format("Invalid Harness User Group with id: {}", harnessUserGroupId), harnessUserGroup);
//    Query<AccessRequest> query = wingsPersistence.createQuery(AccessRequest.class, excludeAuthority);
//    query.filter("harnessUserGroupId", harnessUserGroupId);
//    query.filter("accessActive", true);
//    return query.asList();
//  }
//
//  @Override
//  public List<AccessRequest> getActiveAccessRequestForAccount(String accountId) {
//    Account account = accountService.get(accountId);
//    notNullCheck("Invalid account with id: " + accountId, account);
//    Query<AccessRequest> query = wingsPersistence.createQuery(AccessRequest.class, excludeAuthority);
//    query.filter("harnessUserGroupId", accountId);
//    query.filter("accessActive", true);
//    return query.asList();
//  }
//
//  @Override
//  public AccessRequest update(String accessRequestId, long startTime, long endTime) {
//    AccessRequest accessRequest = wingsPersistence.get(AccessRequest.class, accessRequestId);
//    notNullCheck(String.format("Invalid Access Request with id: {}", accessRequestId), accessRequest);
//    return accessRequest;
//  }

  @Override
  public boolean delete(String accessRequestId) {
    return wingsPersistence.delete(AccessRequest.class, accessRequestId);
  }

//  @Override
//  public AccessRequest updateStatus(String accessRequestId, boolean acccessStatus){
//    AccessRequest accessRequest = AccessRequest.builder().build();
//    return accessRequest;
//  }
}
