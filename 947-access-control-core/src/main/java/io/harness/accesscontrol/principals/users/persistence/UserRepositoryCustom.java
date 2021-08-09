package io.harness.accesscontrol.principals.users.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface UserRepositoryCustom {
  long insertAllIgnoringDuplicates(List<UserDBO> users);
}
