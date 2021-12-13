package io.harness.ng.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.events.AbstractDelegateConfigurationEvent;
import io.harness.ng.core.Resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.DEL)
@Getter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateConfigurationCreateEvent extends AbstractDelegateConfigurationEvent {
  private DelegateProfileDetailsNg delegateProfile;

  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(delegateProfile.getIdentifier())
        .type(ResourceType.DELEGATE_CONFIGURATION.name())
        .build();
  }

  @Override
  public String getEventType() {
    return "DelegateProfileCreated";
  }
}
