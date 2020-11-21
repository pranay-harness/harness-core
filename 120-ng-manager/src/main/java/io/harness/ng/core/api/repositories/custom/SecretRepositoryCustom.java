package io.harness.ng.core.api.repositories.custom;

import io.harness.ng.core.models.Secret;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface SecretRepositoryCustom {
  Page<Secret> findAll(Criteria criteria, Pageable pageable);
}
