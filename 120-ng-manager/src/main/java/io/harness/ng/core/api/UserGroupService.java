package io.harness.ng.core.api;

import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.UserGroup;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserGroupService {
  UserGroup create(UserGroupDTO userGroup);

  Optional<UserGroup> get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  UserGroup update(UserGroupDTO userGroupDTO);

  Page<UserGroup> list(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, Pageable pageable);

  List<UserGroup> list(UserGroupFilterDTO userGroupFilterDTO);

  UserGroup delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean checkMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier);

  UserGroup addMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier);

  UserGroup removeMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier);
}
