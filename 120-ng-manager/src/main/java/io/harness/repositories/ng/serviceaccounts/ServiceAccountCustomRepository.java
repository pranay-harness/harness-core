package io.harness.repositories.ng.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface ServiceAccountCustomRepository {
  Page<ServiceAccount> findAll(Criteria criteria, Pageable pageable);
}
