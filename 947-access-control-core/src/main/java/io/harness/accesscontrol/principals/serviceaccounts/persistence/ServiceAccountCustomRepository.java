package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
public interface ServiceAccountCustomRepository {
  long deleteMulti(Criteria criteria);
}
