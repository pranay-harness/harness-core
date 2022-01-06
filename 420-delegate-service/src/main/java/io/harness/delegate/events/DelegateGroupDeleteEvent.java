/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
import io.harness.delegate.beans.DelegateSetupDetails;
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
public class DelegateGroupDeleteEvent extends AbstractDelegateConfigurationEvent {
  private DelegateSetupDetails delegateSetupDetails;
  private String delegateGroupId;

  @Override
  public Resource getResource() {
    return Resource.builder().identifier(delegateGroupId).type(ResourceTypeConstants.DELEGATE).build();
  }

  @Override
  public String getEventType() {
    return "DelegateGroupDeleteEvent";
  }
}
