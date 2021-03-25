package io.harness.ng.core.user.services.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.invites.spring.UserProjectMapRepository;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  @Inject private UserClient userClient;
  @Inject private UserProjectMapRepository userProjectMapRepository;

  @Override
  public Page<UserInfo> list(String accountIdentifier, String searchString, Pageable pageable) {
    //  @Ankush remove the offset and limit from the following statement because it is redundant pagination
    PageResponse<UserInfo> userPageResponse = RestClientUtils.getResponse(userClient.list(
        accountIdentifier, String.valueOf(pageable.getOffset()), String.valueOf(pageable.getPageSize()), searchString));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public List<UserProjectMap> listUserProjectMap(Criteria criteria) {
    return userProjectMapRepository.findAll(criteria);
  }

  // isSignedUp flag: Users in current gen exists even if they are in invited state and not signed up yet.
  public Optional<UserInfo> getUserFromEmail(String email) {
    return RestClientUtils.getResponse(userClient.getUserFromEmail(email));
  }

  public List<String> getUsernameFromEmail(String accountIdentifier, List<String> emailList) {
    return RestClientUtils.getResponse(userClient.getUsernameFromEmail(accountIdentifier, emailList));
  }

  @Override
  public Optional<UserProjectMap> getUserProjectMap(
      String uuid, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return userProjectMapRepository.findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        uuid, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public boolean createUserProjectMap(Invite invite, UserInfo user) {
    Optional<UserProjectMap> userProjectMapOptional =
        userProjectMapRepository.findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            user.getUuid(), invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier());

    UserProjectMap userProjectMap = userProjectMapOptional
                                        .map(e -> {
                                          e.getRoles().add(invite.getRole());
                                          return e;
                                        })
                                        .orElseGet(()
                                                       -> UserProjectMap.builder()
                                                              .userId(user.getUuid())
                                                              .accountIdentifier(invite.getAccountIdentifier())
                                                              .orgIdentifier(invite.getOrgIdentifier())
                                                              .projectIdentifier(invite.getProjectIdentifier())
                                                              .roles(ImmutableList.of(invite.getRole()))
                                                              .build());
    userProjectMapRepository.save(userProjectMap);
    return postCreation(invite, user);
  }

  private boolean postCreation(Invite invite, UserInfo user) {
    String accountId = invite.getAccountIdentifier();
    String userId = user.getUuid();
    return RestClientUtils.getResponse(userClient.addUserToAccount(userId, accountId));
  }

  @Override
  public List<UserInfo> getUsersByIds(List<String> userIds) {
    return RestClientUtils.getResponse(userClient.getUsersByIds(userIds));
  }

  @Override
  public UserProjectMap createUserProjectMap(UserProjectMap userProjectMap) {
    return userProjectMapRepository.save(userProjectMap);
  }

  @Override
  public boolean isUserInAccount(String accountId, String userId) {
    return Boolean.TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public void removeUserFromScope(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<UserProjectMap> userProjectMapOptional =
        userProjectMapRepository.findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            userId, accountIdentifier, orgIdentifier, projectIdentifier);
    if (!userProjectMapOptional.isPresent()) {
      return;
    }
    userProjectMapRepository.delete(userProjectMapOptional.get());
    userProjectMapOptional = userProjectMapRepository.findFirstByUserIdAndAccountIdentifier(userId, accountIdentifier);
    if (!userProjectMapOptional.isPresent()) {
      //      User has been totally removed from the account -> notify cg
      RestClientUtils.getResponse(userClient.safeDeleteUser(userId, accountIdentifier));
    }
  }
}
