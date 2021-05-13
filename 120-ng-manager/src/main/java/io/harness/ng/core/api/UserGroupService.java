package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
public interface UserGroupService {
  UserGroup create(UserGroupDTO userGroup);

  Optional<UserGroup> get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  UserGroup update(UserGroupDTO userGroupDTO);

  Page<UserGroup> list(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, Pageable pageable);

  List<UserGroup> list(UserGroupFilterDTO userGroupFilterDTO);

  PageResponse<UserMetadataDTO> listUsersInUserGroup(
      Scope scope, String userGroupIdentifier, UserFilter userFilter, PageRequest pageRequest);

  UserGroup delete(Scope scope, String identifier);

  boolean checkMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier);

  UserGroup addMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier);

  UserGroup removeMember(Scope scope, String userGroupIdentifier, String userIdentifier);

  void removeMemberAll(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String userIdentifier);
}
