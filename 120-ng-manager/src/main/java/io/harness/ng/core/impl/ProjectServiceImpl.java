package io.harness.ng.core.impl;

import static io.harness.NGCommonEntityConstants.MONGODB_ID;
import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import io.harness.ModuleType;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.DefaultOrganization;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.auditevent.ProjectCreateEvent;
import io.harness.ng.core.auditevent.ProjectDeleteEvent;
import io.harness.ng.core.auditevent.ProjectRestoreEvent;
import io.harness.ng.core.auditevent.ProjectUpdateEvent;
import io.harness.ng.core.beans.ProjectsPerOrganizationCount;
import io.harness.ng.core.beans.ProjectsPerOrganizationCount.ProjectsPerOrganizationCountKeys;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.core.spring.ProjectRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ProjectServiceImpl implements ProjectService {
  private static final String PROJECT_ADMIN_ROLE = "_project_admin";
  private final ProjectRepository projectRepository;
  private final OrganizationService organizationService;
  private final OutboxService outboxService;
  private final NgUserService ngUserService;
  private final TransactionTemplate transactionTemplate;
  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);
  private final AccessControlAdminClient accessControlAdminClient;

  @Inject
  public ProjectServiceImpl(ProjectRepository projectRepository, OrganizationService organizationService,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, NgUserService ngUserService,
      OutboxService outboxService, AccessControlAdminClient accessControlAdminClient) {
    this.projectRepository = projectRepository;
    this.organizationService = organizationService;
    this.transactionTemplate = transactionTemplate;
    this.ngUserService = ngUserService;
    this.outboxService = outboxService;
    this.accessControlAdminClient = accessControlAdminClient;
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

      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        Project savedProject = projectRepository.save(project);
        outboxService.save(new ProjectCreateEvent(project.getAccountIdentifier(), ProjectMapper.writeDTO(project)));
        log.info(String.format("Project with identifier %s and orgIdentifier %s was successfully created",
            project.getIdentifier(), projectDTO.getOrgIdentifier()));
        performActionsPostProjectCreation(project);
        return savedProject;
      }));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("A project with identifier %s and orgIdentifier %s is already present or was deleted",
              project.getIdentifier(), orgIdentifier),
          USER_SRE, ex);
    }
  }

  private void performActionsPostProjectCreation(Project project) {
    log.info(String.format(
        "Performing actions post project creation for project with identifier %s and orgIdentifier %s ...",
        project.getIdentifier(), project.getOrgIdentifier()));
    updateUserMembership(project);
    addUserToAdmins(project);
    log.info(String.format(
        "Successfully completed actions post project creation for project with identifier %s and orgIdentifier %s",
        project.getIdentifier(), project.getOrgIdentifier()));
  }

  private void addUserToAdmins(Project project) {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      String userId = SourcePrincipalContextBuilder.getSourcePrincipal().getName();
      RoleAssignmentDTO roleAssignmentDTO =
          RoleAssignmentDTO.builder()
              .roleIdentifier(PROJECT_ADMIN_ROLE)
              .disabled(false)
              .principal(PrincipalDTO.builder()
                             .type(io.harness.accesscontrol.principals.PrincipalType.USER)
                             .identifier(userId)
                             .build())
              .resourceGroupIdentifier(getFullScopeDefaultResourceGroup(project))
              .build();
      getResponse(accessControlAdminClient.createRoleAssignment(
          project.getAccountIdentifier(), project.getOrgIdentifier(), project.getIdentifier(), roleAssignmentDTO));
    }
  }

  private String getFullScopeDefaultResourceGroup(Project project) {
    return String.format("_%s", project.getIdentifier());
  }

  private void updateUserMembership(Project project) {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      String userId = SourcePrincipalContextBuilder.getSourcePrincipal().getName();
      ngUserService.addUserToScope(userId,
          UserMembership.Scope.builder()
              .accountIdentifier(project.getAccountIdentifier())
              .orgIdentifier(project.getOrgIdentifier())
              .projectIdentifier(project.getIdentifier())
              .build());
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
      if (project.getVersion() == null) {
        project.setVersion(existingProject.getVersion());
      }

      List<ModuleType> moduleTypeList = verifyModulesNotRemoved(existingProject.getModules(), project.getModules());
      project.setModules(moduleTypeList);
      validate(project);
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        Project updatedProject = projectRepository.save(project);
        log.info(String.format(
            "Project with identifier %s and orgIdentifier %s was successfully updated", identifier, orgIdentifier));
        outboxService.save(new ProjectUpdateEvent(
            project.getAccountIdentifier(), ProjectMapper.writeDTO(updatedProject), ProjectMapper.writeDTO(project)));
        return updatedProject;
      }));
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

  @Override
  public List<Project> list(Criteria criteria) {
    return projectRepository.findAll(criteria);
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
          Criteria.where(ProjectKeys.identifier).regex(projectFilterDTO.getSearchTerm(), "i"),
          Criteria.where(ProjectKeys.tags + "." + NGTagKeys.key).regex(projectFilterDTO.getSearchTerm(), "i"),
          Criteria.where(ProjectKeys.tags + "." + NGTagKeys.value).regex(projectFilterDTO.getSearchTerm(), "i"));
    }
    if (isNotEmpty(projectFilterDTO.getIdentifiers())) {
      criteria.and(ProjectKeys.identifier).in(projectFilterDTO.getIdentifiers());
    }
    return criteria;
  }

  @Override
  @DefaultOrganization
  public boolean delete(String accountIdentifier, @OrgIdentifier String orgIdentifier,
      @ProjectIdentifier String projectIdentifier, Long version) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      Project deletedProject = projectRepository.delete(accountIdentifier, orgIdentifier, projectIdentifier, version);
      boolean delete = deletedProject != null;

      if (delete) {
        log.info(String.format("Project with identifier %s and orgIdentifier %s was successfully deleted",
            projectIdentifier, orgIdentifier));
        outboxService.save(
            new ProjectDeleteEvent(deletedProject.getAccountIdentifier(), ProjectMapper.writeDTO(deletedProject)));

      } else {
        log.error(String.format(
            "Project with identifier %s and orgIdentifier %s could not be deleted", projectIdentifier, orgIdentifier));
      }
      return delete;
    }));
  }

  @Override
  public boolean restore(String accountIdentifier, String orgIdentifier, String identifier) {
    validateParentOrgExists(accountIdentifier, orgIdentifier);
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      Project restoredProject = projectRepository.restore(accountIdentifier, orgIdentifier, identifier);
      boolean success = restoredProject != null;
      if (success) {
        outboxService.save(
            new ProjectRestoreEvent(restoredProject.getAccountIdentifier(), ProjectMapper.writeDTO(restoredProject)));
      }
      return success;
    }));
  }

  @Override
  public Map<String, Integer> getProjectsCountPerOrganization(String accountIdentifier, List<String> orgIdentifiers) {
    Criteria criteria =
        Criteria.where(ProjectKeys.accountIdentifier).is(accountIdentifier).and(ProjectKeys.deleted).ne(Boolean.TRUE);
    if (isNotEmpty(orgIdentifiers)) {
      criteria.and(ProjectKeys.orgIdentifier).in(orgIdentifiers);
    }
    MatchOperation matchStage = Aggregation.match(criteria);
    SortOperation sortStage = sort(Sort.by(ProjectKeys.orgIdentifier));
    GroupOperation groupByOrganizationStage =
        group(ProjectKeys.orgIdentifier).count().as(ProjectsPerOrganizationCountKeys.count);
    ProjectionOperation projectionStage =
        project().and(MONGODB_ID).as(ProjectKeys.orgIdentifier).andInclude(ProjectsPerOrganizationCountKeys.count);
    Map<String, Integer> result = new HashMap<>();
    projectRepository
        .aggregate(newAggregation(matchStage, sortStage, groupByOrganizationStage, projectionStage),
            ProjectsPerOrganizationCount.class)
        .getMappedResults()
        .forEach(projectsPerOrganizationCount
            -> result.put(projectsPerOrganizationCount.getOrgIdentifier(), projectsPerOrganizationCount.getCount()));
    return result;
  }

  private void validateCreateProjectRequest(String accountIdentifier, String orgIdentifier, ProjectDTO project) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(orgIdentifier, project.getOrgIdentifier())), true);
    validateParentOrgExists(accountIdentifier, orgIdentifier);
  }

  private void validateParentOrgExists(String accountIdentifier, String orgIdentifier) {
    if (!organizationService.get(accountIdentifier, orgIdentifier).isPresent()) {
      throw new InvalidArgumentsException(
          String.format("Organization [%s] in Account [%s] does not exist", orgIdentifier, accountIdentifier),
          USER_SRE);
    }
  }

  private void validateUpdateProjectRequest(
      String accountIdentifier, String orgIdentifier, String identifier, ProjectDTO project) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(orgIdentifier, project.getOrgIdentifier())), true);
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(identifier, project.getIdentifier())), false);
    validateParentOrgExists(accountIdentifier, orgIdentifier);
  }
}