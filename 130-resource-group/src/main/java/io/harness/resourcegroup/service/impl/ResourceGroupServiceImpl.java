package io.harness.resourcegroup.service.impl;

import io.harness.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupResponse;
import io.harness.resourcegroup.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.resource.validator.ResourceGroupValidatorService;
import io.harness.resourcegroup.service.ResourceGroupService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    if (validate(resourceGroup)) {
      return ResourceGroupMapper.toResponseWrapper(resourceGroupRepository.save(resourceGroup));
    } else {
      log.error("PreValidations failed for resource group {}", resourceGroup);
    }
    return null;
  }

  private boolean validate(ResourceGroup resourceGroup) {
    boolean isValid = staticResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    isValid = isValid && dynamicResourceGroupValidatorService.isResourceGroupValid(resourceGroup);
    return isValid;
  }

  @Override
  public Optional<ResourceGroupResponse> delete(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.deleteByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
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
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
  }

  @Override
  public Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO) {
    ResourceGroup resourceGroup = ResourceGroupMapper.fromDTO(resourceGroupDTO);
    Optional<ResourceGroup> resourceGroupOpt =
        resourceGroupRepository.findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            resourceGroup.getIdentifier(), resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
            resourceGroup.getProjectIdentifier());
    if (!resourceGroupOpt.isPresent()) {
      return Optional.empty();
    }
    ResourceGroup savedResourceGroup = resourceGroupOpt.get();
    savedResourceGroup.setName(resourceGroup.getName());
    savedResourceGroup.setResourceSelectors(resourceGroup.getResourceSelectors());
    savedResourceGroup.setDescription(resourceGroup.getDescription());
    resourceGroup = resourceGroupRepository.save(savedResourceGroup);
    return Optional.ofNullable(ResourceGroupMapper.toResponseWrapper(resourceGroup));
  }
}
