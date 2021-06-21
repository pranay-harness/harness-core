package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface ServiceAccountRepositoryCustom {
  long insertAllIgnoringDuplicates(List<ServiceAccountDBO> serviceAccountDBOS);
}
