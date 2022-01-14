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
public class ApiKeyCreateEvent implements Event {
  public static final String API_KEY_CREATED = "ApiKeyCreated";
  private ApiKeyDTO apiKey;

  public ApiKeyCreateEvent(ApiKeyDTO apiKeyDTO) {
    this.apiKey = apiKeyDTO;
  }

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    return ResourceScopeMapper.getResourceScope(
        Scope.of(apiKey.getAccountIdentifier(), apiKey.getOrgIdentifier(), apiKey.getProjectIdentifier()));
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, apiKey.getName());
    return Resource.builder().identifier(apiKey.getIdentifier()).type(API_KEY).labels(labels).build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return API_KEY_CREATED;
  }
}
