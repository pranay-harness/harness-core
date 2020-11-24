package io.harness.ng.core.impl;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.ng.core.utils.NGUtils.getConnectorRequestDTO;
import static io.harness.ng.core.utils.NGUtils.getDefaultHarnessSecretManagerName;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.core.DefaultOrganization;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.repositories.core.spring.ProjectRepository;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
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
  public Project create(String accountIdentifier, String orgIdentifier, ProjectDTO projectDTO) {
    orgIdentifier = orgIdentifier == null ? DEFAULT_ORG_IDENTIFIER : orgIdentifier;
    validateCreateProjectRequest(accountIdentifier, orgIdentifier, projectDTO);
    Project project = toProject(projectDTO);
    project.setOrgIdentifier(orgIdentifier);
    project.setAccountIdentifier(accountIdentifier);
    try {
      validate(project);
      Project savedProject = projectRepository.save(project);
      performActionsPostProjectCreation(project);
      return savedProject;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Try using different project identifier, [%s] cannot be used", project.getIdentifier()),
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
  @DefaultOrganization
  public Optional<Project> get(
      String accountIdentifier, @OrgIdentifier String orgIdentifier, @ProjectIdentifier String projectIdentifier) {
    return projectRepository.findByAccountIdentifierAndOrgIdentifierAndIdentifierAndDeletedNot(
        accountIdentifier, orgIdentifier, projectIdentifier, true);
  }

  @Override
  @DefaultOrganization
  public Project update(String accountIdentifier, @OrgIdentifier String orgIdentifier,
      @ProjectIdentifier String identifier, ProjectDTO projectDTO) {
    validateUpdateProjectRequest(accountIdentifier, orgIdentifier, identifier, projectDTO);
    Optional<Project> optionalProject = get(accountIdentifier, orgIdentifier, identifier);

    if (optionalProject.isPresent()) {
      Project existingProject = optionalProject.get();
      Project project = toProject(projectDTO);
      project.setAccountIdentifier(accountIdentifier);
      project.setOrgIdentifier(orgIdentifier);
      project.setId(existingProject.getId());

      List<ModuleType> moduleTypeList = verifyModulesNotRemoved(existingProject.getModules(), project.getModules());
      project.setModules(moduleTypeList);
      validate(project);
      return projectRepository.save(project);
    }
    throw new InvalidRequestException(
        String.format("Project with identifier [%s] and orgIdentifier [%s] not found", identifier, orgIdentifier),
        USER);
  }

  private List<ModuleType> verifyModulesNotRemoved(List<ModuleType> oldList, List<ModuleType> newList) {
    Set<ModuleType> oldSet = new HashSet<>(oldList);
    Set<ModuleType> newSet = new HashSet<>(newList);

    if (newSet.containsAll(oldSet)) {
      return new ArrayList<>(newSet);
    }
    throw new InvalidRequestException("Modules cannot be removed from a project");
  }

  @Override
  public Page<Project> list(String accountIdentifier, Pageable pageable, ProjectFilterDTO projectFilterDTO) {
    Criteria criteria = createProjectFilterCriteria(
        Criteria.where(ProjectKeys.accountIdentifier).is(accountIdentifier).and(ProjectKeys.deleted).ne(Boolean.TRUE),
        projectFilterDTO);
    return projectRepository.findAll(criteria, pageable);
  }

  @Override
  public Page<Project> list(Criteria criteria, Pageable pageable) {
    return projectRepository.findAll(criteria, pageable);
  }

  private Criteria createProjectFilterCriteria(Criteria criteria, ProjectFilterDTO projectFilterDTO) {
    if (projectFilterDTO == null) {
      return criteria;
    }
    if (isNotBlank(projectFilterDTO.getOrgIdentifier())) {
      criteria.and(ProjectKeys.orgIdentifier).is(projectFilterDTO.getOrgIdentifier());
    }
    if (projectFilterDTO.getModuleType() != null) {
      if (Boolean.TRUE.equals(projectFilterDTO.getHasModule())) {
        criteria.and(ProjectKeys.modules).in(projectFilterDTO.getModuleType());
      } else {
        criteria.and(ProjectKeys.modules).nin(projectFilterDTO.getModuleType());
      }
    }
    if (isNotBlank(projectFilterDTO.getSearchTerm())) {
      criteria.orOperator(Criteria.where(ProjectKeys.name).regex(projectFilterDTO.getSearchTerm(), "i"),
          Criteria.where(ProjectKeys.tags).regex(projectFilterDTO.getSearchTerm(), "i"));
    }
    return criteria;
  }

  @Override
  @DefaultOrganization
  public boolean delete(String accountIdentifier, @OrgIdentifier String orgIdentifier,
      @ProjectIdentifier String projectIdentifier, Long version) {
    return projectRepository.delete(accountIdentifier, orgIdentifier, projectIdentifier, version);
  }

  private void validateCreateProjectRequest(String accountIdentifier, String orgIdentifier, ProjectDTO project) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, project.getAccountIdentifier()),
                               Pair.of(orgIdentifier, project.getOrgIdentifier())),
        true);
    if (!organizationService.get(accountIdentifier, orgIdentifier).isPresent()) {
      throw new InvalidArgumentsException(
          String.format("Organization [%s] in Account [%s] does not exist", orgIdentifier, accountIdentifier),
          USER_SRE);
    }
  }

  private void validateUpdateProjectRequest(
      String accountIdentifier, String orgIdentifier, String identifier, ProjectDTO project) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, project.getAccountIdentifier()),
                               Pair.of(orgIdentifier, project.getOrgIdentifier())),
        true);
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(identifier, project.getIdentifier())), false);
  }
}