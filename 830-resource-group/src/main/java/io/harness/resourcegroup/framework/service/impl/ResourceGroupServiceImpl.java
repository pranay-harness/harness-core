package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
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
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;
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
  ResourceGroupValidatorServiceImpl resourceGroupValidatorService;
  ResourceGroupRepository resourceGroupRepository;
  OutboxService outboxService;
  AccessControlAdminClient accessControlAdminClient;
  TransactionTemplate transactionTemplate;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public ResourceGroupServiceImpl(ResourceGroupValidatorServiceImpl resourceGroupValidatorService,
      ResourceGroupRepository resourceGroupRepository, OutboxService outboxService,
      AccessControlAdminClient accessControlAdminClient,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
    this.resourceGroupValidatorService = resourceGroupValidatorService;
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
    resourceGroupValidatorService.validate(resourceGroup);
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      ResourceGroup savedResourceGroup = resourceGroupRepository.save(resourceGroup);
      outboxService.save(new ResourceGroupCreateEvent(
          savedResourceGroup.getAccountIdentifier(), ResourceGroupMapper.toDTO(savedResourceGroup)));
      return savedResourceGroup;
    }));
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

  @Override
  public boolean delete(String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      boolean forceDeleteRoleAssignments) {
    Optional<ResourceGroup> resourceGroupOpt =
        getResourceGroup(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
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
        if (!forceDeleteRoleAssignments) {
          throw new InvalidRequestException(
              "There exists role assignments with this resource group. Please delete them first and then try again");
        } else {
          PaginationUtils.forEachElement(counter
              -> NGRestUtils.getResponse(accessControlAdminClient.getFilteredRoleAssignments(
                  accountIdentifier, orgIdentifier, projectIdentifier, 0, 20, roleAssignmentFilterDTO)),
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
    Optional<ResourceGroup> resourceGroupOpt =
        getResourceGroup(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
  }

  private Optional<ResourceGroup> getResourceGroup(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return resourceGroupRepository
        .findOneByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndDeleted(
            identifier, accountIdentifier, orgIdentifier, projectIdentifier, false);
  }

  @Override
  public Page<ResourceGroup> list(Criteria criteria, Pageable pageable) {
    return resourceGroupRepository.findAll(criteria, pageable);
  }

  @Override
  public Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    Optional<ResourceGroup> resourceGroupOpt = getResourceGroup(resourceGroup.getIdentifier(),
        resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier());
    if (!resourceGroupOpt.isPresent()) {
      throw new InvalidRequestException(String.format(
          "Resource group with Identifier [{%s}] in Scope {%s} does not exists", resourceGroupDTO.getIdentifier(),
          ScopeUtils.toString(resourceGroupDTO.getAccountIdentifier(), resourceGroupDTO.getOrgIdentifier(),
              resourceGroupDTO.getProjectIdentifier())));
    }
    ResourceGroup savedResourceGroup = resourceGroupOpt.get();
    if (savedResourceGroup.getHarnessManaged().equals(TRUE)) {
      throw new InvalidRequestException("Can't update managed resource group");
    }
    preprocessResourceGroup(resourceGroup);
    resourceGroupValidatorService.validate(resourceGroup);
    ResourceGroupDTO oldResourceGroup =
        (ResourceGroupDTO) NGObjectMapperHelper.clone(ResourceGroupMapper.toDTO(savedResourceGroup));
    savedResourceGroup.setName(resourceGroup.getName());
    savedResourceGroup.setColor(resourceGroup.getColor());
    savedResourceGroup.setTags(resourceGroup.getTags());
    savedResourceGroup.setDescription(resourceGroup.getDescription());
    savedResourceGroup.setFullScopeSelected(resourceGroup.getFullScopeSelected());
    savedResourceGroup.setResourceSelectors(collectResourceSelectors(resourceGroup.getResourceSelectors()));
    resourceGroup = Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      ResourceGroup updatedResourceGroup = resourceGroupRepository.save(savedResourceGroup);
      outboxService.save(new ResourceGroupUpdateEvent(savedResourceGroup.getAccountIdentifier(),
          ResourceGroupMapper.toDTO(updatedResourceGroup), oldResourceGroup));
      return updatedResourceGroup;
    }));
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroup));
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
            .collect(toMap(StaticResourceSelector::getResourceType, StaticResourceSelector::getIdentifiers,
                (oldResourceIds, newResourceIds) -> {
                  oldResourceIds.addAll(newResourceIds);
                  return oldResourceIds;
                }));

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

  public boolean restoreAll(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ResourceGroupKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(ResourceGroupKeys.deleted)
                            .is(true);
    Update update = new Update().set(ResourceGroupKeys.deleted, false);
    return resourceGroupRepository.update(criteria, update);
  }
}
