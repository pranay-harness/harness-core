package io.harness.resourcegroup.resourceclient.organization;

import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static java.util.stream.Collectors.toList;

import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.Scope;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class OrganizationResourceValidatorImpl implements ResourceValidator {
  OrganizationManagerClient organizationManagerClient;

  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PageResponse<OrganizationResponse> organizations =
        getResponse(organizationManagerClient.listOrganizations(accountIdentifier, resourceIds));
    Set<String> validResourceIds =
        organizations.getContent().stream().map(e -> e.getOrganization().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream()
        .map(resourceId -> validResourceIds.contains(resourceId) && orgIdentifier.equals(resourceId))
        .collect(toList());
  }

  @Override
  public EnumSet<ValidatorType> getValidatorTypes() {
    return EnumSet.of(STATIC);
  }

  @Override
  public String getResourceType() {
    return "ORGANIZATION";
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ORGANIZATION);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY);
  }

  @Override
  public ResourcePrimaryKey getResourceGroupKeyFromEvent(Message message) {
    OrganizationEntityChangeDTO organizationEntityChangeDTO = null;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking OrganizationEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(organizationEntityChangeDTO)) {
      return null;
    }
    return ResourcePrimaryKey.builder()
        .accountIdentifier(organizationEntityChangeDTO.getAccountIdentifier())
        .orgIdentifier(organizationEntityChangeDTO.getIdentifier())
        .resourceType(getResourceType())
        .resourceIdetifier(organizationEntityChangeDTO.getIdentifier())
        .build();
  }
}
