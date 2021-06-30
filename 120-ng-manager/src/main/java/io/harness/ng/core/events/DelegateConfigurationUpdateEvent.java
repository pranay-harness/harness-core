package io.harness.ng.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
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
public class DelegateConfigurationUpdateEvent extends AbstractDelegateConfigurationEvent {
  private DelegateProfileDetailsNg oldProfile;
  private DelegateProfileDetailsNg newProfile;

  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(newProfile.getUuid())
        .type(ResourceTypeConstants.DELEGATE_CONFIGURATION)
        .build();
  }

  @Override
  public String getEventType() {
    return "DelegateProfileUpdated";
  }
}
