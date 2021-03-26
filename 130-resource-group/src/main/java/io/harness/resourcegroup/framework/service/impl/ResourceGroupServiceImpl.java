package io.harness.resourcegroup.framework.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ACCOUNT;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ORGANIZATION;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.PROJECT;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.beans.SortOrder;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.resourcegroup.ResourceGroupEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.remote.client.NGRestUtils;
import io.harness.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupBuilder;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector.StaticResourceSelectorKeys;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResourceGroupServiceImpl implements ResourceGroupService {
  private static final String DEFAULT_COLOR = "#0063F7";
  ResourceGroupValidatorService staticResourceGroupValidatorService;
  ResourceGroupValidatorService dynamicResourceGroupValidatorService;
  ResourceGroupRepository resourceGroupRepository;
  Producer eventProducer;
  Map<String, ResourceValidator> resourceValidators;
  AccessControlAdminClient accessControlAdminClient;

  @Inject
  public ResourceGroupServiceImpl(
      @Named("StaticResourceValidator") ResourceGroupValidatorService staticResourceGroupValidatorService,
      @Named("DynamicResourceValidator") ResourceGroupValidatorService dynamicResourceGroupValidatorService,
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators,
      ResourceGroupRepository resourceGroupRepository,
      @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
      AccessControlAdminClient accessControlAdminClient) {
    this.staticResourceGroupValidatorService = staticResourceGroupValidatorService;
    this.dynamicResourceGroupValidatorService = dynamicResourceGroupValidatorService;
    this.resourceValidators = resourceValidators;
    this.resourceGroupRepository = resourceGroupRepository;
    this.eventProducer = eventProducer;
    this.accessControlAdminClient = accessControlAdminClient;
  }

  @Override
  public ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    ResourceGroup createdResourceGroup = create(resourceGroup);
    return ResourceGroupMapper.toResponseWrapper(createdResourceGroup);
  }

  private ResourceGroup create(ResourceGroup resourceGroup) {
    preprocessResourceGroup(resourceGroup);
    if (validate(resourceGroup)) {
      try {
        ResourceGroup savedResourceGroup = resourceGroupRepository.save(resourceGroup);
        publishEvent(resourceGroup, EventsFrameworkMetadataConstants.CREATE_ACTION);
        return savedResourceGroup;
      } catch (DuplicateKeyException ex) {
        throw new DuplicateFieldException(
            String.format("A resource group with identifier %s already exists at the specified scope",
                resourceGroup.getIdentifier()),
            USER_SRE, ex);
      }
    }
    log.error("PreValidations failed for resource group {}", resourceGroup);
    throw new InvalidRequestException("Prevalidation Checks failed for the resource group");
  }

  private void publishEvent(ResourceGroup resourceGroup, String action) {
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", resourceGroup.getAccountIdentifier(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.RESOURCE_GROUP,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(getResourceGroupPayload(resourceGroup))
              .build());
    } catch (ProducerShutdownException e) {
      log.error(
          "Failed to send event to events framework for resourcegroup Identifier {}", resourceGroup.getIdentifier(), e);
    }
  }

  private ByteString getResourceGroupPayload(ResourceGroup resourceGroup) {
    ResourceGroupEntityChangeDTO.Builder resourceGroupEntityChangeDTOBuilder =
        ResourceGroupEntityChangeDTO.newBuilder()
            .setIdentifier(resourceGroup.getIdentifier())
            .setAccountIdentifier(resourceGroup.getAccountIdentifier());
    if (isNotBlank(resourceGroup.getOrgIdentifier())) {
      resourceGroupEntityChangeDTOBuilder.setOrgIdentifier(resourceGroup.getOrgIdentifier());
    }
    if (isNotBlank(resourceGroup.getProjectIdentifier())) {
      resourceGroupEntityChangeDTOBuilder.setProjectIdentifier(resourceGroup.getProjectIdentifier());
    }
    return resourceGroupEntityChangeDTOBuilder.build().toByteString();
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
                            .is(accountIdentifier)
                            .and(ResourceGroupKeys.orgIdentifier)
                            .is(orgIdentifier)
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
    if (TRUE.equals(resourceGroup.getFullScopeSelected()) && isNotEmpty(resourceGroup.getResourceSelectors())) {
      return false;
    }

    boolean isValid = staticResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    isValid = isValid && dynamicResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    return isValid;
  }

  @Override
  public boolean delete(String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository
            .findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
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
        throw new InvalidRequestException(
            "There exists role assignments with this resource group. Please delete them first and then try again");
      }
      resourceGroup.setDeleted(true);
      resourceGroupRepository.save(resourceGroup);
      publishEvent(resourceGroupOpt.get(), EventsFrameworkMetadataConstants.DELETE_ACTION);
    }
    return true;
  }

  @Override
  @SuppressWarnings("PMD")
  public Optional<ResourceGroupResponse> find(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    accountIdentifier = StringUtils.stripToNull(accountIdentifier);
    orgIdentifier = StringUtils.stripToNull(orgIdentifier);
    projectIdentifier = StringUtils.stripToNull(projectIdentifier);

    if (accountIdentifier == null) {
      throw new NullPointerException("Account Identifier can't be null");
    }
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository
            .findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
                identifier, accountIdentifier, orgIdentifier, projectIdentifier, false);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
  }

  @Override
  public Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    if (resourceGroup.getHarnessManaged().equals(TRUE)) {
      return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroup));
    }
    resourceGroup.setResourceSelectors(collectResourceSelectors(resourceGroup.getResourceSelectors()));
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository
            .findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
                resourceGroup.getIdentifier(), resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
                resourceGroup.getProjectIdentifier(), false);
    if (!resourceGroupOpt.isPresent()) {
      return Optional.empty();
    }
    ResourceGroup savedResourceGroup = resourceGroupOpt.get();
    savedResourceGroup.setName(resourceGroup.getName());
    savedResourceGroup.setColor(resourceGroup.getColor());
    savedResourceGroup.setTags(resourceGroup.getTags());
    savedResourceGroup.setDescription(resourceGroup.getDescription());
    if (validate(resourceGroup)) {
      savedResourceGroup.setFullScopeSelected(resourceGroup.getFullScopeSelected());
      savedResourceGroup.setResourceSelectors(resourceGroup.getResourceSelectors());
    }
    resourceGroup = resourceGroupRepository.save(savedResourceGroup);
    publishEvent(resourceGroup, EventsFrameworkMetadataConstants.UPDATE_ACTION);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroup));
  }

  @Override
  public boolean handleResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey) {
    Criteria criteria = getCriteriaForResourceDeleteEvent(resourcePrimaryKey);
    Update update = getUpdateForResourceDeleteEvent(resourcePrimaryKey);
    return resourceGroupRepository.update(criteria, update);
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
    return false;
  }

  private Update getUpdateForResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey) {
    String resourceType = resourcePrimaryKey.getResourceType();
    Update update = new Update();
    if (resourceType.equals(ACCOUNT) || resourceType.equals(ORGANIZATION) || resourceType.equals(PROJECT)) {
      update = update.set(ResourceGroupKeys.deleted, true);
    } else {
      update = update.pull(ResourceGroupKeys.resourceSelectors + ".$." + StaticResourceSelectorKeys.identifiers,
          resourcePrimaryKey.getResourceIdetifier());
      log.info("{}", update);
    }
    return update;
  }

  private Criteria getCriteriaForResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey) {
    String resourceType = resourcePrimaryKey.getResourceType();
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .is(resourcePrimaryKey.getAccountIdentifier())
                            .and(ResourceGroupKeys.deleted)
                            .is(false);

    if (isNotBlank(resourcePrimaryKey.getOrgIdentifier())) {
      criteria.and(ResourceGroupKeys.orgIdentifier).is(resourcePrimaryKey.getOrgIdentifier());
    }
    if (isNotBlank(resourcePrimaryKey.getProjectIdentifer())) {
      criteria.and(ResourceGroupKeys.projectIdentifier).is(resourcePrimaryKey.getProjectIdentifer());
    }

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
      String description = String.format("All the resources in this %s are included in this resource group.",
          StringUtils.capitalize(resourcePrimaryKey.getResourceType().toLowerCase()));

      ResourceGroupBuilder resourceGroupBuilder = ResourceGroup.builder()
                                                      .accountIdentifier(resourcePrimaryKey.getAccountIdentifier())
                                                      .orgIdentifier(resourcePrimaryKey.getOrgIdentifier())
                                                      .projectIdentifier(resourcePrimaryKey.getProjectIdentifer())
                                                      .name(resourcePrimaryKey.getResourceIdetifier())
                                                      .description(description)
                                                      .harnessManaged(true)
                                                      .fullScopeSelected(true);
      String resourceGroupIdentifier = String.format("_%s", resourcePrimaryKey.getResourceIdetifier());
      boolean created = false;
      int counter = 1;
      do {
        ResourceGroup resourceGroup = resourceGroupBuilder.identifier(resourceGroupIdentifier).build();
        try {
          create(resourceGroup);
          created = true;
        } catch (DuplicateFieldException e) {
          log.info("Identifier {} for default resource group already taken", resourceGroupIdentifier);
        } catch (Exception e) {
          log.info("Failed to create default resource groups", e);
          break;
        }
        resourceGroupIdentifier = String.format("_%s_%d", resourcePrimaryKey.getResourceIdetifier(), counter++);
      } while (!created);
      return created;
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
                              .and(ResourceGroupKeys.deleted)
                              .is(true);

      if (isNotBlank(resourcePrimaryKey.getOrgIdentifier())) {
        criteria.and(ResourceGroupKeys.orgIdentifier).is(resourcePrimaryKey.getOrgIdentifier());
      }
      if (isNotBlank(resourcePrimaryKey.getProjectIdentifer())) {
        criteria.and(ResourceGroupKeys.projectIdentifier).is(resourcePrimaryKey.getProjectIdentifer());
      }
      Update update = new Update().set(ResourceGroupKeys.deleted, false);
      return resourceGroupRepository.update(criteria, update);
    }
    return true;
  }
}
