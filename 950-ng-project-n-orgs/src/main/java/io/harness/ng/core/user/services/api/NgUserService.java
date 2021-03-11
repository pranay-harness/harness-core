package io.harness.ng.core.user.services.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.user.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NgUserService {
  Page<User> list(String accountIdentifier, String searchString, Pageable page);

  PageResponse<UserSearchDTO> listUsersAtScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest);

  List<UserMembership> listUserMemberships(Criteria criteria);

  List<User> getUsersByIds(List<String> userIds);

  Optional<User> getUserFromEmail(String emailIds);

  List<User> getUsersFromEmail(List<String> emailIds);

  List<String> getUsersHavingRole(Scope scope, String roleIdentifier);

  Optional<UserMembership> getUserMembership(String userId);

  void addUserToScope(User user, Scope scope);

  void addUserToScope(String userId, String emailId, Scope scope);

  UserMembership addUserToScope(String userId, Scope scope);

  boolean isUserInAccount(String accountId, String userId);

  boolean removeUserMembership(String userId);

  void removeScope(String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
