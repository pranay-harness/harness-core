package io.harness.repositories.core.custom;

import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class OrganizationRepositoryCustomImpl implements OrganizationRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Organization> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Organization> organizations = mongoTemplate.find(query, Organization.class);
    return PageableExecutionUtils.getPage(
        organizations, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Organization.class));
  }

  @Override
  public List<Organization> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, Organization.class);
  }

  @Override
  public Organization update(Query query, Update update) {
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Organization.class);
  }

  @Override
  public Boolean delete(String accountIdentifier, String identifier, Long version) {
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(OrganizationKeys.identifier)
                            .is(identifier)
                            .and(OrganizationKeys.harnessManaged)
                            .ne(Boolean.TRUE)
                            .and(OrganizationKeys.deleted)
                            .ne(Boolean.TRUE);
    if (version != null) {
      criteria.and(OrganizationKeys.version).is(version);
    }
    Query query = new Query(criteria);
    Update update = new Update().set(OrganizationKeys.deleted, Boolean.TRUE);
    return mongoTemplate.findAndModify(query, update, Organization.class) != null;
  }
}
