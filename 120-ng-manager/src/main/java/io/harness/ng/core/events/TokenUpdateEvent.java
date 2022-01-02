package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.TOKEN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.event.Event;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.mapper.ResourceScopeMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class TokenUpdateEvent implements Event {
  public static final String TOKEN_UPDATED = "TokenUpdated";
  private TokenDTO oldToken;
  private TokenDTO newToken;

  public TokenUpdateEvent(TokenDTO oldToken, TokenDTO newToken) {
    this.oldToken = oldToken;
    this.newToken = newToken;
  }

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    return ResourceScopeMapper.getResourceScope(
        Scope.of(newToken.getAccountIdentifier(), newToken.getOrgIdentifier(), newToken.getProjectIdentifier()));
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newToken.getName());
    return Resource.builder().identifier(oldToken.getIdentifier()).type(TOKEN).labels(labels).build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return TOKEN_UPDATED;
  }
}
