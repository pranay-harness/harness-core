package io.harness.ng.core.services.api.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.dao.api.repositories.ProjectRepository;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.api.ProjectService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class ProjectServiceImpl implements ProjectService {
  private final ProjectRepository projectRepository;
  private final MongoTemplate mongoTemplate;

  @Override
  public Project create(@NotNull @Valid Project project) {
    try {
      return projectRepository.save(project);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format("Project [%s] under Organization [%s] already exists",
                                            project.getIdentifier(), project.getOrgId()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<Project> get(@NotNull String projectId) {
    return projectRepository.findByIdAndDeletedNot(projectId, Boolean.TRUE);
  }

  @Override
  public Project update(@Valid Project project) {
    Objects.requireNonNull(project.getId());
    return projectRepository.save(project);
  }

  @Override
  public Page<Project> list(@NotNull String organizationId, @NotNull Criteria criteria, Pageable pageable) {
    criteria = criteria.and(ProjectKeys.orgId).is(organizationId);
    return list(criteria, pageable);
  }

  @Override
  public Page<Project> list(@NotNull Criteria criteria, @NotNull Pageable pageable) {
    criteria = criteria.and(ProjectKeys.deleted).ne(Boolean.TRUE);
    Query query = new Query(criteria).with(pageable);
    List<Project> projects = mongoTemplate.find(query, Project.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Project.class));
  }

  @Override
  public boolean delete(String projectId) {
    Optional<Project> projectOptional = get(projectId);
    if (projectOptional.isPresent()) {
      Project project = projectOptional.get();
      project.setDeleted(Boolean.TRUE);
      projectRepository.save(project);
      return true;
    }
    return false;
  }
}
