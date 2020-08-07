package io.harness.ng.core.dao.api.repositories.custom;

import com.google.inject.Inject;

import io.harness.ng.core.entities.Project;
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
public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Project> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Project> projects = mongoTemplate.find(query, Project.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Project.class));
  }
}
