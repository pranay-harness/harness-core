package io.harness.audit.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface AuditYamlRepositoryCustom {
  void delete(Criteria criteria);
}
