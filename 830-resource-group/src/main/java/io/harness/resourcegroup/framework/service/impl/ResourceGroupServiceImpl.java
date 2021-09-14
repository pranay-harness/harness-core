/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.SortOrder;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.resourcegroup.framework.events.ResourceGroupCreateEvent;
import io.harness.resourcegroup.framework.events.ResourceGroupDeleteEvent;
import io.harness.resourcegroup.framework.events.ResourceGroupUpdateEvent;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@OwnedBy(PL)
public class ResourceGroupServiceImpl implements ResourceGroupService {
  ResourceGroupValidatorServiceImpl resourceGroupValidatorService;
  ResourceGroupRepository resourceGroupRepository;
  OutboxService outboxService;
  TransactionTemplate transactionTemplate;

  @Inject
  public ResourceGroupServiceImpl(ResourceGroupValidatorServiceImpl resourceGroupValidatorService,
      ResourceGroupRepository resourceGroupRepository, OutboxService outboxService,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
    this.resourceGroupValidatorService = resourceGroupValidatorService;
    this.resourceGroupRepository = resourceGroupRepository;
    this.outboxService = outboxService;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    try {
      ResourceGroup createdResourceGroup = create(resourceGroup);
      return ResourceGroupMapper.toResponseWrapper(createdResourceGroup);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("A resource group with identifier %s already exists at the specified scope",
              resourceGroup.getIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public void createManagedResourceGroup(Scope scope) {
    try {
      create(ResourceGroup.getHarnessManagedResourceGroup(scope));
    } catch (DuplicateKeyException ex) {
      // Ignore
    }
  }

  private ResourceGroup create(ResourceGroup resourceGroup) {
    boolean sanitized = resourceGroupValidatorService.sanitizeResourceSelectors(resourceGroup);
    if (sanitized && resourceGroup.getResourceSelectors().isEmpty()) {
      throw new InvalidRequestException("All selected resources are invalid");
    }
    return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      ResourceGroup savedResourceGroup = resourceGroupRepository.save(resourceGroup);
      outboxService.save(new ResourceGroupCreateEvent(
          savedResourceGroup.getAccountIdentifier(), ResourceGroupMapper.toDTO(savedResourceGroup)));
      return savedResourceGroup;
    }));
  }

  private Criteria getResourceGroupCritera(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm) {
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .in(accountIdentifier)
                            .and(ResourceGroupKeys.orgIdentifier)
                            .in(orgIdentifier)
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(projectIdentifier);
    if (Objects.nonNull(stripToNull(searchTerm))) {
      criteria.orOperator(Criteria.where(ResourceGroupKeys.name).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.identifier).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.tags + "." + NGTagKeys.key).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.tags + "." + NGTagKeys.value).regex(searchTerm, "i"));
    }
    return criteria;
  }

  @Override
  public Page<ResourceGroupResponse> list(Scope scope, PageRequest pageRequest, String searchTerm) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder harnessManagedOrder =
          SortOrder.Builder.aSortOrder().withField(ResourceGroupKeys.harnessManaged, SortOrder.OrderType.DESC).build();
      SortOrder lastModifiedOrder =
          SortOrder.Builder.aSortOrder().withField(ResourceGroupKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(harnessManagedOrder, lastModifiedOrder));
    }
    Pageable page = getPageRequest(pageRequest);
    Criteria criteria = getResourceGroupCritera(
        scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), searchTerm);
    return resourceGroupRepository.findAll(criteria, page).map(ResourceGroupMapper::toResponseWrapper);
  }

  @Override
  public Page<ResourceGroupResponse> list(ResourceGroupFilterDTO resourceGroupFilterDTO, PageRequest pageRequest) {
    Criteria criteria = getResourceGroupCritera(resourceGroupFilterDTO.getAccountIdentifier(),
        resourceGroupFilterDTO.getOrgIdentifier(), resourceGroupFilterDTO.getProjectIdentifier(),
        resourceGroupFilterDTO.getSearchTerm());
    if (isNotEmpty(resourceGroupFilterDTO.getIdentifierFilter())) {
      criteria.and(ResourceGroupKeys.identifier).in(resourceGroupFilterDTO.getIdentifierFilter());
    }
    return resourceGroupRepository.findAll(criteria, getPageRequest(pageRequest))
        .map(ResourceGroupMapper::toResponseWrapper);
  }

  @Override
  public void delete(Scope scope, String identifier) {
    Optional<ResourceGroup> resourceGroupOpt = getResourceGroup(scope, identifier);
    if (!resourceGroupOpt.isPresent()) {
      return;
    }

    ResourceGroup resourceGroup = resourceGroupOpt.get();
    if (Boolean.TRUE.equals(resourceGroup.getHarnessManaged())) {
      throw new InvalidRequestException("Managed resource group cannot be deleted");
    }

    Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      resourceGroupRepository.delete(resourceGroup);
      outboxService.save(
          new ResourceGroupDeleteEvent(scope.getAccountIdentifier(), ResourceGroupMapper.toDTO(resourceGroup)));
      return true;
    }));
  }

  @Override
  public void deleteByScope(Scope scope) {
    if (scope == null || isEmpty(scope.getAccountIdentifier())) {
      throw new InvalidRequestException("Invalid scope. Cannot proceed with deletion.");
    }
    Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      List<ResourceGroup> deletedResourceGroups =
          resourceGroupRepository.deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
              scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
      if (isNotEmpty(deletedResourceGroups)) {
        deletedResourceGroups.forEach(rg
            -> outboxService.save(
                new ResourceGroupDeleteEvent(rg.getAccountIdentifier(), ResourceGroupMapper.toDTO(rg))));
      }
      return true;
    }));
  }

  @Override
  @SuppressWarnings("PMD")
  public Optional<ResourceGroupResponse> get(Scope scope, String identifier) {
    Optional<ResourceGroup> resourceGroupOpt = getResourceGroup(scope, identifier);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
  }

  private Optional<ResourceGroup> getResourceGroup(Scope scope, String identifier) {
    return resourceGroupRepository.findOneByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        identifier, scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  @Override
  public Page<ResourceGroup> list(Criteria criteria, Pageable pageable) {
    return resourceGroupRepository.findAll(criteria, pageable);
  }

  @Override
  public Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO, boolean sanitizeResourceSelectors) {
    Optional<ResourceGroup> resourceGroupOpt =
        getResourceGroup(resourceGroupDTO.getScope(), resourceGroupDTO.getIdentifier());
    if (!resourceGroupOpt.isPresent()) {
      throw new InvalidRequestException(
          String.format("Resource group with Identifier [{%s}] in Scope {%s} does not exist",
              resourceGroupDTO.getIdentifier(), resourceGroupDTO.getScope()));
    }
    ResourceGroup updatedResourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    if (sanitizeResourceSelectors) {
      resourceGroupValidatorService.sanitizeResourceSelectors(updatedResourceGroup);
    }
    ResourceGroup savedResourceGroup = resourceGroupOpt.get();
    if (savedResourceGroup.getHarnessManaged().equals(TRUE)) {
      throw new InvalidRequestException("Can't update managed resource group");
    }

    ResourceGroupDTO oldResourceGroup =
        (ResourceGroupDTO) NGObjectMapperHelper.clone(ResourceGroupMapper.toDTO(savedResourceGroup));

    savedResourceGroup.setName(updatedResourceGroup.getName());
    savedResourceGroup.setColor(updatedResourceGroup.getColor());
    savedResourceGroup.setTags(updatedResourceGroup.getTags());
    savedResourceGroup.setDescription(updatedResourceGroup.getDescription());
    savedResourceGroup.setFullScopeSelected(updatedResourceGroup.getFullScopeSelected());
    savedResourceGroup.setResourceSelectors(updatedResourceGroup.getResourceSelectors());

    updatedResourceGroup =
        Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
          ResourceGroup resourceGroup = resourceGroupRepository.save(savedResourceGroup);
          outboxService.save(new ResourceGroupUpdateEvent(
              savedResourceGroup.getAccountIdentifier(), ResourceGroupMapper.toDTO(resourceGroup), oldResourceGroup));
          return resourceGroup;
        }));
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(updatedResourceGroup));
  }
}
