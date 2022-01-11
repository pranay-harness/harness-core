package io.harness.resourcegroup.resourceclient.gitops;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.template.TemplateSummaryResponseDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class AgentResourceImpl implements Resource {
  private final GitopsResourceClient gitopsResourceClient;

  @Override
  public String getType() {
    return "GITOPS_AGENT";
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return EnumSet.of(ScopeLevel.PROJECT, ScopeLevel.ORGANIZATION, ScopeLevel.ACCOUNT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.GITOPS_AGENT_ENTITY);
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    EntityChangeDTO entityChangeDTO = null;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(entityChangeDTO)) {
      return null;
    }
    return ResourceInfo.builder()
        .accountIdentifier(stripToNull(entityChangeDTO.getAccountIdentifier().getValue()))
        .orgIdentifier(stripToNull(entityChangeDTO.getOrgIdentifier().getValue()))
        .projectIdentifier(stripToNull(entityChangeDTO.getProjectIdentifier().getValue()))
        .resourceType(getType())
        .resourceIdentifier(entityChangeDTO.getIdentifier().getValue())
        .build();
  }

  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    if (EmptyPredicate.isEmpty(resourceIds)) {
      return Collections.EMPTY_LIST;
    }
    final List<String> agents =
        NGRestUtils
            .getResponse(gitopsResourceClient.listAgents(scope.getAccountIdentifier(), scope.getOrgIdentifier(),
                scope.getProjectIdentifier(), 0, resourceIds.size(), null))
            .getContent();
    Set<Object> validResourceIds = new HashSet<>(agents);
    return resourceIds.stream().map(validResourceIds::contains).collect(toList());
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(ValidatorType.DYNAMIC);
  }
}
