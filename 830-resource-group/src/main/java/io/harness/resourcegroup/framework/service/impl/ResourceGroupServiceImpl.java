package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ACCOUNT;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ORGANIZATION;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.PROJECT;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.framework.events.ResourceGroupCreateEvent;
import io.harness.resourcegroup.framework.events.ResourceGroupDeleteEvent;
import io.harness.resourcegroup.framework.events.ResourceGroupUpdateEvent;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector.StaticResourceSelectorKeys;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.utils.PaginationUtils;
import io.harness.utils.RetryUtils;
import io.harness.utils.ScopeUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@OwnedBy(PL)
public class ResourceGroupServiceImpl implements ResourceGroupService {
  private static final String DEFAULT_COLOR = "#0063F7";
  private static final String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private static final String DESCRIPTION_FORMAT = "All the resources in this %s are included in this resource group.";
  ResourceGroupValidatorService staticResourceGroupValidatorService;
  ResourceGroupValidatorService dynamicResourceGroupValidatorService;
  ResourceGroupRepository resourceGroupRepository;
  OutboxService outboxService;
  Map<String, ResourceValidator> resourceValidators;
  AccessControlAdminClient accessControlAdminClient;
  TransactionTemplate transactionTemplate;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public ResourceGroupServiceImpl(
      @Named("StaticResourceValidator") ResourceGroupValidatorService staticResourceGroupValidatorService,
      @Named("DynamicResourceValidator") ResourceGroupValidatorService dynamicResourceGroupValidatorService,
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators,
      ResourceGroupRepository resourceGroupRepository, OutboxService outboxService,
      AccessControlAdminClient accessControlAdminClient,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
    this.staticResourceGroupValidatorService = staticResourceGroupValidatorService;
    this.dynamicResourceGroupValidatorService = dynamicResourceGroupValidatorService;
    this.resourceValidators = resourceValidators;
    this.resourceGroupRepository = resourceGroupRepository;
    this.outboxService = outboxService;
    this.accessControlAdminClient = accessControlAdminClient;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    ResourceGroup createdResourceGroup;
    try {
      createdResourceGroup = create(resourceGroup);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("A resource group with identifier %s already exists at the specified scope",
              resourceGroup.getIdentifier()),
          USER_SRE, ex);
    }
    return ResourceGroupMapper.toResponseWrapper(createdResourceGroup);
  }

  @Override
  public ResourceGroupResponse createManagedResourceGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    resourceGroup.setHarnessManaged(true);
    ResourceGroup createdResourceGroup = null;
    try {
      createdResourceGroup = create(resourceGroup);
    } catch (DuplicateKeyException ex) {
      log.error("Resource group with identifier {}/{} already present",
          ScopeUtils.toString(accountIdentifier, orgIdentifier, projectIdentifier), resourceGroupDTO.getIdentifier());
    }
    return ResourceGroupMapper.toResponseWrapper(createdResourceGroup);
  }

  private ResourceGroup create(ResourceGroup resourceGroup) {
    preprocessResourceGroup(resourceGroup);
    if (validate(resourceGroup)) {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        ResourceGroup savedResourceGroup = resourceGroupRepository.save(resourceGroup);
        outboxService.save(new ResourceGroupCreateEvent(
            savedResourceGroup.getAccountIdentifier(), ResourceGroupMapper.toDTO(savedResourceGroup)));
        return savedResourceGroup;
      }));
    }
    log.error("PreValidations failed for resource group {}", resourceGroup);
    throw new InvalidRequestException("Prevalidation Checks failed for the resource group");
  }

  @Override
  public Page<ResourceGroupResponse> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, String searchTerm) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ResourceGroupKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Pageable page = getPageRequest(pageRequest);
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .in(accountIdentifier)
                            .and(ResourceGroupKeys.orgIdentifier)
                            .in(orgIdentifier)
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(ResourceGroupKeys.deleted)
                            .is(false);
    if (Objects.nonNull(stripToNull(searchTerm))) {
      criteria.orOperator(Criteria.where(ResourceGroupKeys.name).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.identifier).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.tags + "." + NGTagKeys.key).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.tags + "." + NGTagKeys.value).regex(searchTerm, "i"));
    }
    return resourceGroupRepository.findAll(criteria, page).map(ResourceGroupMapper::toResponseWrapper);
  }

  private boolean validate(ResourceGroup resourceGroup) {
    if ((isBlank(resourceGroup.getIdentifier()) || resourceGroup.getIdentifier().charAt(0) == '_')
        && !TRUE.equals(resourceGroup.getHarnessManaged())) {
      throw new InvalidRequestException(
          "Identifiers starting with _ are only allowed for Harness managed resource group");
    }
    if (TRUE.equals(resourceGroup.getFullScopeSelected()) && isNotEmpty(resourceGroup.getResourceSelectors())) {
      return false;
    }
    boolean isValid = staticResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    isValid = isValid && dynamicResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    return isValid;
  }

  @Override
  public boolean delete(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier, boolean safeDelete) {
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.findOneByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
            identifier, accountIdentifier, orgIdentifier, projectIdentifier, false);
    if (resourceGroupOpt.isPresent()) {
      ResourceGroup resourceGroup = resourceGroupOpt.get();
      RoleAssignmentFilterDTO roleAssignmentFilterDTO =
          RoleAssignmentFilterDTO.builder()
              .resourceGroupFilter(Collections.singleton(resourceGroup.getIdentifier()))
              .build();
      PageResponse<RoleAssignmentResponseDTO> pageResponse =
          NGRestUtils.getResponse(accessControlAdminClient.getFilteredRoleAssignments(
              accountIdentifier, orgIdentifier, projectIdentifier, 0, 10, roleAssignmentFilterDTO));
      if (pageResponse.getPageItemCount() > 0) {
        if (safeDelete) {
          throw new InvalidRequestException(
              "There exists role assignments with this resource group. Please delete them first and then try again");
        } else {
          PaginationUtils.forEachElement(counter
              -> NGRestUtils.getResponse(accessControlAdminClient.getFilteredRoleAssignments(
                  accountIdentifier, orgIdentifier, projectIdentifier, counter, 20, roleAssignmentFilterDTO)),
              roleAssignmentResponseDTO
              -> NGRestUtils.getResponse(accessControlAdminClient.deleteRoleAssignment(
                  roleAssignmentResponseDTO.getRoleAssignment().getIdentifier(), accountIdentifier, orgIdentifier,
                  projectIdentifier)));
        }
      }
      resourceGroup.setDeleted(true);
      Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        resourceGroupRepository.save(resourceGroup);
        outboxService.save(new ResourceGroupDeleteEvent(accountIdentifier, ResourceGroupMapper.toDTO(resourceGroup)));
        return true;
      }));
    }
    return true;
  }

  @Override
  @SuppressWarnings("PMD")
  public Optional<ResourceGroupResponse> get(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    accountIdentifier = StringUtils.stripToNull(accountIdentifier);
    orgIdentifier = StringUtils.stripToNull(orgIdentifier);
    projectIdentifier = StringUtils.stripToNull(projectIdentifier);

    if (accountIdentifier == null) {
      throw new NullPointerException("Account Identifier can't be null");
    }
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.findOneByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
            identifier, accountIdentifier, orgIdentifier, projectIdentifier, false);
    if (!resourceGroupOpt.isPresent() && identifier.equals(DEFAULT_RESOURCE_GROUP_IDENTIFIER)) {
      return Optional.ofNullable(createDefaultResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier));
    }
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
  }

  private ResourceGroupResponse createDefaultResourceGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ResourceGroupDTO resourceGroupDTO =
        ResourceGroupDTO.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(DEFAULT_RESOURCE_GROUP_NAME)
            .identifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
            .description(String.format(DESCRIPTION_FORMAT,
                ScopeUtils.getMostSignificantScope(accountIdentifier, orgIdentifier, projectIdentifier)
                    .toString()
                    .toLowerCase()))
            .resourceSelectors(Collections.emptyList())
            .fullScopeSelected(true)
            .build();
    return createManagedResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupDTO);
  }

  @Override
  public Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.findOneByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
            resourceGroup.getIdentifier(), resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
            resourceGroup.getProjectIdentifier(), false);
    if (!resourceGroupOpt.isPresent()) {
      return Optional.empty();
    }
    ResourceGroup savedResourceGroup = resourceGroupOpt.get();
    if (savedResourceGroup.getHarnessManaged().equals(TRUE)) {
      throw new InvalidRequestException("Can't update managed resource group");
    }
    ResourceGroupDTO oldResourceGroup =
        (ResourceGroupDTO) NGObjectMapperHelper.clone(ResourceGroupMapper.toDTO(savedResourceGroup));
    savedResourceGroup.setName(resourceGroup.getName());
    savedResourceGroup.setColor(resourceGroup.getColor());
    savedResourceGroup.setTags(resourceGroup.getTags());
    savedResourceGroup.setDescription(resourceGroup.getDescription());
    if (validate(resourceGroup)) {
      savedResourceGroup.setFullScopeSelected(resourceGroup.getFullScopeSelected());
      savedResourceGroup.setResourceSelectors(collectResourceSelectors(resourceGroup.getResourceSelectors()));
    }
    resourceGroup = Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      ResourceGroup updatedResourceGroup = resourceGroupRepository.save(savedResourceGroup);
      outboxService.save(new ResourceGroupUpdateEvent(savedResourceGroup.getAccountIdentifier(),
          ResourceGroupMapper.toDTO(updatedResourceGroup), oldResourceGroup));
      return updatedResourceGroup;
    }));
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroup));
  }

  @Override
  public boolean handleResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey) {
    Criteria criteria = getCriteriaForResourceDeleteEvent(resourcePrimaryKey);
    String resourceType = resourcePrimaryKey.getResourceType();
    boolean shouldDeleteResourceGroup =
        resourceType.equals(ACCOUNT) || resourceType.equals(ORGANIZATION) || resourceType.equals(PROJECT);
    int pageCounter = 0;
    int maxLimit = 50;
    while (pageCounter < maxLimit) {
      Pageable pageable = org.springframework.data.domain.PageRequest.of(pageCounter, 20);
      Page<ResourceGroup> resourceGroupsPage = resourceGroupRepository.findAll(criteria, pageable);
      if (!resourceGroupsPage.hasContent()) {
        break;
      }
      for (ResourceGroup resourceGroup : resourceGroupsPage.getContent()) {
        if (shouldDeleteResourceGroup) {
          delete(resourceGroup.getIdentifier(), resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
              resourceGroup.getProjectIdentifier(), false);
        } else {
          ResourceGroup oldResourceGroup = (ResourceGroup) NGObjectMapperHelper.clone(resourceGroup);
          deleteResourceFromGroup(resourcePrimaryKey, resourceType, resourceGroup);
          Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
            ResourceGroup updatedResourceGroup = resourceGroupRepository.save(resourceGroup);
            outboxService.save(new ResourceGroupUpdateEvent(resourcePrimaryKey.getAccountIdentifier(),
                ResourceGroupMapper.toDTO(updatedResourceGroup), ResourceGroupMapper.toDTO(oldResourceGroup)));
            return updatedResourceGroup;
          }));
        }
      }
      pageCounter++;
    }
    return true;
  }

  private void deleteResourceFromGroup(
      ResourcePrimaryKey resourcePrimaryKey, String resourceType, ResourceGroup resourceGroup) {
    Optional<StaticResourceSelector> resourceSelectorOpt = resourceGroup.getResourceSelectors()
                                                               .stream()
                                                               .filter(StaticResourceSelector.class ::isInstance)
                                                               .map(StaticResourceSelector.class ::cast)
                                                               .filter(rs -> rs.getResourceType().equals(resourceType))
                                                               .findFirst();
    if (!resourceSelectorOpt.isPresent()) {
      throw new IllegalStateException("Panic situation. Must have staticresourceselector for " + resourceType);
    }
    StaticResourceSelector resourceSelector = resourceSelectorOpt.get();
    boolean isRemoved = resourceSelector.getIdentifiers().remove(resourcePrimaryKey.getResourceIdetifier());
    if (!isRemoved) {
      throw new IllegalStateException(
          "Panic situation. Must have resourceIdentifier " + resourcePrimaryKey.getResourceIdetifier());
    }
    if (resourceSelector.getIdentifiers().isEmpty()) {
      resourceGroup.getResourceSelectors().remove(resourceSelector);
    }
  }

  @Override
  public boolean deleteStaleResources(ResourceGroup resourceGroup) {
    Map<String, List<String>> staticResourceSelectors =
        resourceGroup.getResourceSelectors()
            .stream()
            .filter(StaticResourceSelector.class ::isInstance)
            .map(StaticResourceSelector.class ::cast)
            .collect(toMap(StaticResourceSelector::getResourceType, StaticResourceSelector::getIdentifiers));
    staticResourceSelectors.forEach((resourceType, value) -> {
      if (resourceValidators.containsKey(resourceType)) {
        value.forEach(resourceId -> {
          boolean exists = resourceValidators.get(resourceType)
                               .validate(Collections.singletonList(resourceId), resourceGroup.getAccountIdentifier(),
                                   resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier())
                               .get(0);
          if (!exists) {
            ResourcePrimaryKey resourcePrimaryKey = ResourcePrimaryKey.builder()
                                                        .accountIdentifier(resourceGroup.getAccountIdentifier())
                                                        .orgIdentifier(resourceGroup.getOrgIdentifier())
                                                        .projectIdentifer(resourceGroup.getProjectIdentifier())
                                                        .resourceType(resourceType)
                                                        .resourceIdetifier(resourceId)
                                                        .build();
            handleResourceDeleteEvent(resourcePrimaryKey);
          }
        });
      }
    });
    return true;
  }

  private Criteria getCriteriaForResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey) {
    String resourceType = resourcePrimaryKey.getResourceType();
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .is(resourcePrimaryKey.getAccountIdentifier())
                            .and(ResourceGroupKeys.orgIdentifier)
                            .is(resourcePrimaryKey.getOrgIdentifier())
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(resourcePrimaryKey.getProjectIdentifer())
                            .and(ResourceGroupKeys.deleted)
                            .is(false);

    if (resourceType.equals(ACCOUNT) || resourceType.equals(ORGANIZATION) || resourceType.equals(PROJECT)) {
      return criteria;
    }
    criteria.and(ResourceGroupKeys.resourceSelectors + "." + StaticResourceSelectorKeys.resourceType)
        .is(resourcePrimaryKey.getResourceType())
        .and(ResourceGroupKeys.resourceSelectors + "." + StaticResourceSelectorKeys.identifiers)
        .is(resourcePrimaryKey.getResourceIdetifier());
    return criteria;
  }

  public boolean createDefaultResourceGroup(ResourcePrimaryKey resourcePrimaryKey) {
    String resourceType = resourcePrimaryKey.getResourceType();
    if (resourceType.equals(PROJECT) || resourceType.equals(ORGANIZATION) || resourceType.equals(ACCOUNT)) {
      createDefaultResourceGroup(resourcePrimaryKey.getAccountIdentifier(), resourcePrimaryKey.getOrgIdentifier(),
          resourcePrimaryKey.getProjectIdentifer());
    }
    return true;
  }

  void preprocessResourceGroup(ResourceGroup resourceGroup) {
    resourceGroup.setResourceSelectors(collectResourceSelectors(resourceGroup.getResourceSelectors()));
    if (isBlank(resourceGroup.getColor())) {
      resourceGroup.setColor(DEFAULT_COLOR);
    }
  }

  private static List<ResourceSelector> collectResourceSelectors(List<ResourceSelector> resourceSelectors) {
    Map<String, List<String>> resources =
        resourceSelectors.stream()
            .filter(StaticResourceSelector.class ::isInstance)
            .map(StaticResourceSelector.class ::cast)
            .collect(toMap(StaticResourceSelector::getResourceType, StaticResourceSelector::getIdentifiers));
    List<ResourceSelector> condensedResourceSelectors = new ArrayList<>();
    resources.forEach(
        (k, v)
            -> condensedResourceSelectors.add(StaticResourceSelector.builder().resourceType(k).identifiers(v).build()));
    resourceSelectors.stream()
        .filter(DynamicResourceSelector.class ::isInstance)
        .map(DynamicResourceSelector.class ::cast)
        .distinct()
        .forEach(condensedResourceSelectors::add);
    return condensedResourceSelectors;
  }

  public boolean restoreResourceGroupsUnderHierarchy(ResourcePrimaryKey resourcePrimaryKey) {
    String entityType = resourcePrimaryKey.getResourceType();
    boolean resourceHierarchyChanged =
        entityType.equals(ACCOUNT) || entityType.equals(ORGANIZATION) || entityType.equals(PROJECT);
    if (resourceHierarchyChanged) {
      Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                              .is(resourcePrimaryKey.getAccountIdentifier())
                              .and(ResourceGroupKeys.orgIdentifier)
                              .is(resourcePrimaryKey.getOrgIdentifier())
                              .and(ResourceGroupKeys.projectIdentifier)
                              .is(resourcePrimaryKey.getProjectIdentifer())
                              .and(ResourceGroupKeys.deleted)
                              .is(true);
      Update update = new Update().set(ResourceGroupKeys.deleted, false);
      return resourceGroupRepository.update(criteria, update);
    }
    return true;
  }
}
