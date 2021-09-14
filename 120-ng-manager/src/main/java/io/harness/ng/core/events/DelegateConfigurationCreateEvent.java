/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
public class DelegateConfigurationCreateEvent extends AbstractDelegateConfigurationEvent {
  private DelegateProfileDetailsNg delegateProfile;

  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(delegateProfile.getUuid())
        .type(ResourceTypeConstants.DELEGATE_CONFIGURATION)
        .build();
  }

  @Override
  public String getEventType() {
    return "DelegateProfileCreated";
  }
}
