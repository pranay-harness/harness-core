package io.harness.ng.core.services.api.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dao.api.repositories.spring.ProjectRepository;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.api.OrganizationService;
import io.harness.ng.core.services.api.ProjectService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;

import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ProjectServiceImpl implements ProjectService {
  private final ProjectRepository projectRepository;
  private final OrganizationService organizationService;

  @Override
  public Project create(@NotNull @Valid Project project) {
    if (!organizationService.get(project.getAccountIdentifier(), project.getOrgIdentifier()).isPresent()) {
      throw new InvalidArgumentsException(String.format("Organization [%s] in Account [%s] does not exist",
                                              project.getOrgIdentifier(), project.getAccountIdentifier()),
          USER_SRE);
    }
    try {
      return projectRepository.save(project);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format("Project [%s] under Organization [%s] already exists",
                                            project.getIdentifier(), project.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<Project> get(String orgIdentifier, String projectIdentifier) {
    return projectRepository.findByOrgIdentifierAndIdentifierAndDeletedNot(orgIdentifier, projectIdentifier, true);
  }

  @Override
  public Project update(@Valid Project project) {
    Objects.requireNonNull(project.getId());
    return projectRepository.save(project);
  }

  @Override
  public Page<Project> list(@NotNull Criteria criteria, @NotNull Pageable pageable) {
    return projectRepository.findAll(criteria, pageable);
  }

  @Override
  public Page<Project> list(
      @NotNull TextCriteria textCriteria, @NotNull Criteria criteria, @NotNull Pageable pageable) {
    return projectRepository.findAll(textCriteria, criteria, pageable);
  }

  @Override
  public boolean delete(String orgIdentifier, String projectIdentifier) {
    Optional<Project> projectOptional = get(orgIdentifier, projectIdentifier);
    if (projectOptional.isPresent()) {
      Project project = projectOptional.get();
      project.setDeleted(true);
      projectRepository.save(project);
      return true;
    }
    return false;
  }
}
