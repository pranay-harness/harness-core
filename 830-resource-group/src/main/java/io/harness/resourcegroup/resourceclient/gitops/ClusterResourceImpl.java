/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resourcegroup.resourceclient.gitops;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ClusterResourceImpl implements Resource {
  @Override
  public String getType() {
    return "GITOPS_CLUSTER";
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return EnumSet.of(ScopeLevel.PROJECT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.GITOPS_CLUSTER_ENTITY);
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
    return null;
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(ValidatorType.DYNAMIC);
  }
}
