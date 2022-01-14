package io.harness.accesscontrol.scopes.core.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface ScopeRepositoryCustom {
  long insertAllIgnoringDuplicates(List<ScopeDBO> users);
}
