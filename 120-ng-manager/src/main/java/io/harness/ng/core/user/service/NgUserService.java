package io.harness.ng.core.user.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.Scope;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NgUserService {
  List<UserInfo> getUsersByIds(List<String> userIds, String accountIdentifier);

  Optional<UserInfo> getUserById(String userId);

  Optional<UserInfo> getUserFromEmail(String emailIds);

  List<UserInfo> getUsersFromEmail(List<String> emailIds, String accountIdentifier);

  List<String> getUsersHavingRole(Scope scope, String roleIdentifier);

  Optional<UserMembership> getUserMembership(String userId);

  Page<UserInfo> list(String accountIdentifier, String searchString, Pageable page);

  List<String> listUsersAtScope(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  List<UserMembership> listUserMemberships(Criteria criteria);

  void addUserToScope(UserInfo user, Scope scope);

  void addUserToScope(String user, Scope scope, String roleIdentifier);

  void addUserToScope(UserInfo user, Scope scope, boolean postCreation);

  boolean isUserInAccount(String accountId, String userId);

  boolean isUserAtScope(String userId, Scope scope);

  void removeUserFromScope(String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
