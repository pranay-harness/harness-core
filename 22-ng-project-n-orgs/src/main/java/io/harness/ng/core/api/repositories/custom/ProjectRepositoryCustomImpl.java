package io.harness.ng.core.api.repositories.custom;

import com.google.inject.Inject;

import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
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

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Project> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Project> projects = mongoTemplate.find(query, Project.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Project.class));
  }

  @Override
  public Project update(Query query, Update update) {
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Project.class);
  }

  @Override
  public Boolean delete(String accountIdentifier, String orgIdentifier, String identifier, Long version) {
    Criteria criteria = Criteria.where(ProjectKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ProjectKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ProjectKeys.identifier)
                            .is(identifier)
                            .and(ProjectKeys.deleted)
                            .ne(Boolean.TRUE);
    if (version != null) {
      criteria.and(ProjectKeys.version).is(version);
    }
    Query query = new Query(criteria);
    Update update = new Update().set(ProjectKeys.deleted, Boolean.TRUE);
    return mongoTemplate.findAndModify(query, update, Project.class) != null;
  }
}
