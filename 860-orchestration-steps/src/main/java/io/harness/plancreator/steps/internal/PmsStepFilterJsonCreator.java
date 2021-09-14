/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.plancreator.steps.internal;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filters.GenericStepPMSFilterJsonCreator;
import io.harness.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(PIPELINE)
public class PmsStepFilterJsonCreator extends GenericStepPMSFilterJsonCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.BARRIER, StepSpecTypeConstants.HTTP,
        StepSpecTypeConstants.HARNESS_APPROVAL, StepSpecTypeConstants.JIRA_APPROVAL, StepSpecTypeConstants.JIRA_CREATE,
        StepSpecTypeConstants.JIRA_UPDATE, StepSpecTypeConstants.RESOURCE_CONSTRAINT);
  }
}
