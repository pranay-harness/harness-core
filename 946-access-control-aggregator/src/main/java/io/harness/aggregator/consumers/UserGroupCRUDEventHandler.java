package io.harness.aggregator.consumers;

import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface UserGroupCRUDEventHandler {
  void handleUserGroupCreate(@NotNull @Valid UserGroupDBO userGroupDBO);
  void handleUserGroupUpdate(@NotNull @Valid UserGroupDBO userGroupDBO);
  void handleUserGroupDelete(@NotEmpty String id);
}
