package io.harness.repositories.core.custom;

import io.harness.ng.core.entities.Organization;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface OrganizationRepositoryCustom {
  Page<Organization> findAll(Criteria criteria, Pageable pageable);

  Organization update(Query query, Update update);

  Boolean delete(String accountIdentifier, String identifier, Long version);

  List<Organization> findAll(Criteria criteria);
}
