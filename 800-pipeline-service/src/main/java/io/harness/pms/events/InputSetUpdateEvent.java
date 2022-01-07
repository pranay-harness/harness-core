/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PIPELINE)
@Getter
@Builder
@AllArgsConstructor
public class InputSetUpdateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String pipelineIdentifier;
  private InputSetEntity newInputSet;
  private InputSetEntity oldInputSet;

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(newInputSet.getIdentifier())
        .type(ResourceTypeConstants.INPUT_SET)
        .labels(ImmutableMap.<String, String>builder().put("pipelineIdentifier", pipelineIdentifier).build())
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return PipelineOutboxEvents.INPUT_SET_UPDATED;
  }
}
