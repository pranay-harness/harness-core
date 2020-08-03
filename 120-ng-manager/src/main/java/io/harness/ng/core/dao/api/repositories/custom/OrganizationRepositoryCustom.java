package io.harness.ng.core.dao.api.repositories.custom;

import io.harness.ng.core.entities.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;

public interface OrganizationRepositoryCustom {
  Page<Organization> findAll(Criteria criteria, Pageable pageable);
  Page<Organization> findAll(TextCriteria textCriteria, Criteria criteria, Pageable pageable);
}
