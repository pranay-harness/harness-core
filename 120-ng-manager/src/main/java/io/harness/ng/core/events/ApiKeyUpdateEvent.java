/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.API_KEY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.event.Event;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.mapper.ResourceScopeMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ApiKeyUpdateEvent implements Event {
  public static final String API_KEY_UPDATED = "ApiKeyUpdated";
  private ApiKeyDTO oldApiKey;
  private ApiKeyDTO newApiKey;

  public ApiKeyUpdateEvent(ApiKeyDTO oldApiKey, ApiKeyDTO newApiKey) {
    this.oldApiKey = oldApiKey;
    this.newApiKey = newApiKey;
  }

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    return ResourceScopeMapper.getResourceScope(
        Scope.of(newApiKey.getAccountIdentifier(), newApiKey.getOrgIdentifier(), newApiKey.getProjectIdentifier()));
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newApiKey.getName());
    return Resource.builder().identifier(oldApiKey.getIdentifier()).type(API_KEY).labels(labels).build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return API_KEY_UPDATED;
  }
}
