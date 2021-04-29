package io.harness.ng.core.user.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateMechanism;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.Scope;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NgUserService {
  List<UserInfo> getUsersByIds(List<String> userIds, String accountIdentifier);

  Optional<UserInfo> getUserById(String userId);

  Optional<UserInfo> getUserFromEmail(String emailIds);

  List<UserInfo> getUsersFromEmail(List<String> emailIds, String accountIdentifier);

  List<String> getUserIdsWithRole(Scope scope, String roleIdentifier);

  Optional<UserMembership> getUserMembership(String userId);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  Page<UserInfo> listCurrentGenUsers(String accountIdentifier, String searchString, Pageable page);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  PageResponse<UserInfo> listUsers(Scope scope, PageRequest pageRequest);

  List<String> listUserIds(Scope scope);

  /**
   * Use this method with caution, verify that the criteria sort is able to make use of the indexes.
   */
  List<UserMembership> listUserMemberships(Criteria criteria);

  void addUserToScope(UserInfo user, Scope scope, UserMembershipUpdateMechanism mechanism);

  void addUserToScope(String user, Scope scope, String roleIdentifier, UserMembershipUpdateMechanism mechanism);

  void addUserToScope(UserInfo user, Scope scope, boolean postCreation, UserMembershipUpdateMechanism mechanism);

  boolean isUserInAccount(String accountId, String userId);

  boolean isUserAtScope(String userId, Scope scope);

  boolean removeUserFromScope(String userId, Scope scope, UserMembershipUpdateMechanism mechanism);

  boolean removeUserFromAccount(String userId, String accountIdentifier);

  boolean removeUser(String userId);

  Set<String> filterUsersWithScopeMembership(List<String> userIds, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier);

  Page<ProjectDTO> listProjects(String accountId, PageRequest pageRequest);
}
