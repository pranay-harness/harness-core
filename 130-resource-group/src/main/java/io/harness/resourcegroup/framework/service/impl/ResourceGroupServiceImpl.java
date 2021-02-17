package io.harness.resourcegroup.framework.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.beans.SortOrder;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResourceGroupServiceImpl implements ResourceGroupService {
  ResourceGroupValidatorService staticResourceGroupValidatorService;
  ResourceGroupValidatorService dynamicResourceGroupValidatorService;
  ResourceGroupRepository resourceGroupRepository;

  @Inject
  public ResourceGroupServiceImpl(
      @Named("StaticResourceValidator") ResourceGroupValidatorService staticResourceGroupValidatorService,
      @Named("DynamicResourceValidator") ResourceGroupValidatorService dynamicResourceGroupValidatorService,
      ResourceGroupRepository resourceGroupRepository) {
    this.staticResourceGroupValidatorService = staticResourceGroupValidatorService;
    this.dynamicResourceGroupValidatorService = dynamicResourceGroupValidatorService;
    this.resourceGroupRepository = resourceGroupRepository;
  }

  @Override
  public ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup =
        io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper.fromDTO(resourceGroupDTO);
    if (validate(resourceGroup)) {
      return io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper.toResponseWrapper(
          resourceGroupRepository.save(resourceGroup));
    } else {
      log.error("PreValidations failed for resource group {}", resourceGroup);
    }
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
                            .is(accountIdentifier)
                            .and(ResourceGroupKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(projectIdentifier);
    if (Objects.nonNull(stripToNull(searchTerm))) {
      criteria.orOperator(Criteria.where(ResourceGroupKeys.name).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.identifier).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.tags + "." + NGTagKeys.key).regex(searchTerm, "i"),
          Criteria.where(ResourceGroupKeys.tags + "." + NGTagKeys.value).regex(searchTerm, "i"));
    }
    return resourceGroupRepository.findAll(criteria, page)
        .map(io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper::toResponseWrapper);
  }

  private boolean validate(ResourceGroup resourceGroup) {
    boolean isValid = staticResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    isValid = isValid && dynamicResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    return isValid;
  }

  @Override
  public boolean delete(String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Long resourceGroupDeleted =
        resourceGroupRepository.deleteResourceGroupByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return resourceGroupDeleted > 0;
  }

  @Override
  public Optional<ResourceGroupResponse> find(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    accountIdentifier = StringUtils.stripToNull(accountIdentifier);
    orgIdentifier = StringUtils.stripToNull(orgIdentifier);
    projectIdentifier = StringUtils.stripToNull(projectIdentifier);

    if (accountIdentifier == null) {
      throw new NullPointerException("Account Identifier can't be null");
    }
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return Optional.ofNullable(io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper.toResponseWrapper(
        resourceGroupOpt.orElse(null)));
  }

  @Override
  public Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup =
        io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper.fromDTO(resourceGroupDTO);
    if (resourceGroup.getHarnessManaged().equals(TRUE)) {
      return Optional.ofNullable(
          io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper.toResponseWrapper(resourceGroup));
    }
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            resourceGroup.getIdentifier(), resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
            resourceGroup.getProjectIdentifier());
    if (!resourceGroupOpt.isPresent()) {
      return Optional.empty();
    }
    ResourceGroup savedResourceGroup = resourceGroupOpt.get();
    savedResourceGroup.setName(resourceGroup.getName());
    savedResourceGroup.setColor(resourceGroup.getColor());
    savedResourceGroup.setTags(resourceGroup.getTags());
    savedResourceGroup.setResourceSelectors(resourceGroup.getResourceSelectors());
    savedResourceGroup.setDescription(resourceGroup.getDescription());
    resourceGroup = resourceGroupRepository.save(savedResourceGroup);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroup));
  }
}
