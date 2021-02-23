package io.harness.resourcegroup.resourceclient.project;

import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.toList;

import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.resourcegroup.framework.beans.ResourceGroupConstants;
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
public class ProjectResourceValidatorImpl implements ResourceValidator {
  ProjectManagerClient projectManagerClient;

  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PageResponse<ProjectResponse> projects =
        getResponse(projectManagerClient.listProjects(accountIdentifier, orgIdentifier, resourceIds));
    Set<String> validResourcIds =
        projects.getContent().stream().map(e -> e.getProject().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream().map(validResourcIds::contains).collect(toList());
  }

  @Override
  public String getResourceType() {
    return ResourceGroupConstants.PROJECT;
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ORGANIZATION);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.PROJECT_ENTITY);
  }

  @Override
  public ResourcePrimaryKey getResourceGroupKeyFromEvent(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO = null;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ProjectEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(projectEntityChangeDTO)) {
      return null;
    }
    return ResourcePrimaryKey.builder()
        .accountIdentifier(projectEntityChangeDTO.getAccountIdentifier())
        .orgIdentifier(projectEntityChangeDTO.getOrgIdentifier())
        .projectIdentifer(projectEntityChangeDTO.getIdentifier())
        .resourceType(getResourceType())
        .resourceIdetifier(projectEntityChangeDTO.getIdentifier())
        .build();
  }
}
