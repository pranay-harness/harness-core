package io.harness.ng.core.dao.api.repositories.custom;

import com.google.inject.Inject;

import io.harness.ng.core.entities.Organization;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class OrganizationRepositoryCustomImpl implements OrganizationRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Organization> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Organization> organizations = mongoTemplate.find(query, Organization.class);
    return PageableExecutionUtils.getPage(
        organizations, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Organization.class));
  }
}
