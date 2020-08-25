package io.harness.ng.core.impl;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;
import static io.harness.ng.core.utils.NGUtils.getConnectorRequestDTO;
import static io.harness.ng.core.utils.NGUtils.getDefaultHarnessSecretManagerName;
import static io.harness.secretmanagerclient.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.connector.services.ConnectorService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.repositories.spring.ProjectRepository;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.service.impl.security.SecretManagementException;

import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class ProjectServiceImpl implements ProjectService {
  private final ProjectRepository projectRepository;
  private final OrganizationService organizationService;
  private final NGSecretManagerService ngSecretManagerService;
  private final ConnectorService secretManagerConnectorService;

  @Inject
  public ProjectServiceImpl(ProjectRepository projectRepository, OrganizationService organizationService,
      NGSecretManagerService ngSecretManagerService,
      @Named(SECRET_MANAGER_CONNECTOR_SERVICE) ConnectorService secretManagerConnectorService) {
    this.projectRepository = projectRepository;
    this.organizationService = organizationService;
    this.ngSecretManagerService = ngSecretManagerService;
    this.secretManagerConnectorService = secretManagerConnectorService;
  }

  @Override
  public Project create(@NotNull @Valid Project project) {
    if (!organizationService.get(project.getAccountIdentifier(), project.getOrgIdentifier()).isPresent()) {
      throw new InvalidArgumentsException(String.format("Organization [%s] in Account [%s] does not exist",
                                              project.getOrgIdentifier(), project.getAccountIdentifier()),
          USER_SRE);
    }
    try {
      Project savedProject = projectRepository.save(project);
      performActionsPostProjectCreation(project);
      return savedProject;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format("Project [%s] under Organization [%s] already exists",
                                            project.getIdentifier(), project.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  private void performActionsPostProjectCreation(Project project) {
    createHarnessSecretManager(project);
  }

  private void createHarnessSecretManager(Project project) {
    try {
      SecretManagerConfigDTO globalSecretManager =
          ngSecretManagerService.getGlobalSecretManager(project.getAccountIdentifier());
      globalSecretManager.setIdentifier(HARNESS_SECRET_MANAGER_IDENTIFIER);
      globalSecretManager.setDescription("Project: " + project.getName());
      globalSecretManager.setName(getDefaultHarnessSecretManagerName(globalSecretManager.getEncryptionType()));
      globalSecretManager.setProjectIdentifier(project.getIdentifier());
      globalSecretManager.setOrgIdentifier(project.getOrgIdentifier());
      globalSecretManager.setDefault(false);
      secretManagerConnectorService.create(getConnectorRequestDTO(globalSecretManager), project.getAccountIdentifier());
    } catch (Exception ex) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          String.format("Harness Secret Manager for project %s could not be created", project.getName()), ex, USER);
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
  public boolean delete(String orgIdentifier, String projectIdentifier) {
    Optional<Project> projectOptional = get(orgIdentifier, projectIdentifier);
    if (projectOptional.isPresent()) {
      Project project = projectOptional.get();
      project.setDeleted(Boolean.TRUE);
      projectRepository.save(project);
      return true;
    }
    return false;
  }
}
