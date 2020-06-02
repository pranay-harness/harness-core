package io.harness.ng.core.services.api.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.dao.api.ProjectRepository;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.api.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class ProjectServiceImpl implements ProjectService {
  private final ProjectRepository projectRepository;

  @Inject
  ProjectServiceImpl(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

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
    return projectRepository.findById(projectId);
  }

  @Override
  public Project update(@Valid Project project) {
    Objects.requireNonNull(project.getUuid());
    return projectRepository.save(project);
  }

  @Override
  public List<Project> getAll() {
    return Lists.newArrayList(projectRepository.findAll());
  }
}
