package io.harness.ng.core.user.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.user.remote.UserFilterNG;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface NgUserService {
  Optional<UserInfo> getUserById(String userId);

  Optional<UserInfo> getUserFromEmail(String emailIds);

  Optional<UserMetadataDTO> getUserMetadata(String userId);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  Page<UserInfo> listCurrentGenUsers(String accountIdentifier, String searchString, Pageable page);

  List<UserInfo> listCurrentGenUsers(String accountId, UserFilterNG userFilter);

  List<UserMetadataDTO> listUsersHavingRole(Scope scope, String roleIdentifier);

  Optional<UserMembership> getUserMembership(String userId);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  PageResponse<UserMetadataDTO> listUsers(Scope scope, PageRequest pageRequest, UserFilter userFilter);

  List<String> listUserIds(Scope scope);

  /**
   * Use this method with caution, verify that the criteria sort is able to make use of the indexes.
   */
  List<UserMembership> listUserMemberships(Criteria criteria);

  List<UserMetadataDTO> getUserMetadata(List<String> userIds);

  void addUserToScope(String user, Scope scope, String roleIdentifier, UserMembershipUpdateSource source);

  void addUserToScope(String userId, Scope scope, boolean postCreation, UserMembershipUpdateSource source);

  void addUserToScope(
      String userId, Scope scope, List<RoleAssignmentDTO> roleAssignmentDTOs, UserMembershipUpdateSource source);

  boolean isUserInAccount(String accountId, String userId);

  boolean isUserAtScope(String userId, Scope scope);

  boolean update(String userId, Update update);

  boolean removeUserFromScope(String userId, Scope scope, UserMembershipUpdateSource source);

  boolean removeUserFromAccount(String userId, String accountIdentifier);

  boolean removeUser(String userId);

  Set<String> filterUsersWithScopeMembership(List<String> userIds, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier);

  Page<ProjectDTO> listProjects(String accountId, PageRequest pageRequest);

  boolean isUserPasswordSet(String accountIdentifier, String email);
}
