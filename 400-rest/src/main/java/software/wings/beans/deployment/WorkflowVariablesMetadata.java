/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.deployment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Variable;

import java.util.List;
import lombok.Value;

@OwnedBy(CDC)
@Value
public class WorkflowVariablesMetadata {
  private static final String CHANGED_MESSAGE =
      "Workflow Variables have changed since previous execution. Please select new values.";

  private List<Variable> workflowVariables;
  private boolean changed;
  private String changedMessage;

  public WorkflowVariablesMetadata(List<Variable> workflowVariables, boolean changed) {
    this.workflowVariables = workflowVariables;
    this.changed = changed;
    if (changed) {
      this.changedMessage = CHANGED_MESSAGE;
    } else {
      this.changedMessage = null;
    }
  }

  public WorkflowVariablesMetadata(List<Variable> workflowVariables) {
    this(workflowVariables, false);
  }
}
