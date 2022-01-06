/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.event;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.CgEventRule;

import software.wings.graphql.schema.type.QLObject;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLEventRule implements QLObject {
  private CgEventRule.CgRuleType type;
  private QLPipelineRule pipelineRule;
  private QLWorkflowRule workflowRule;

  @Data
  public static class QLPipelineRule {
    private boolean allEvents;
    private boolean allPipelines;
    private List<String> pipelineIds;
    private List<String> events;
  }

  @Data
  public static class QLWorkflowRule {
    private boolean allEvents;
    private boolean allWorkflows;
    private List<String> workflowIds;
    private List<String> events;
  }

  public static CgEventRule toEventRule(QLEventRule rule) {
    if (rule == null) {
      return null;
    }

    CgEventRule eventRule = new CgEventRule();
    eventRule.setType(rule.getType());
    if (rule.getPipelineRule() != null) {
      CgEventRule.PipelineRule pipelineRule = new CgEventRule.PipelineRule();
      pipelineRule.setAllEvents(rule.getPipelineRule().isAllEvents());
      pipelineRule.setAllPipelines(rule.getPipelineRule().isAllPipelines());
      pipelineRule.setEvents(rule.getPipelineRule().getEvents());
      pipelineRule.setPipelineIds(rule.getPipelineRule().getPipelineIds());
      eventRule.setPipelineRule(pipelineRule);
    }

    if (rule.getWorkflowRule() != null) {
      CgEventRule.WorkflowRule workflowRule = new CgEventRule.WorkflowRule();
      workflowRule.setAllEvents(rule.getWorkflowRule().isAllEvents());
      workflowRule.setAllWorkflows(rule.getWorkflowRule().isAllWorkflows());
      workflowRule.setEvents(rule.getWorkflowRule().getEvents());
      workflowRule.setWorkflowIds(rule.getWorkflowRule().getWorkflowIds());
      eventRule.setWorkflowRule(workflowRule);
    }
    return eventRule;
  }

  public static QLEventRule toEventRule(CgEventRule rule) {
    if (rule == null) {
      return null;
    }

    QLEventRule eventRule = new QLEventRule();
    eventRule.setType(rule.getType());
    if (rule.getPipelineRule() != null) {
      QLPipelineRule pipelineRule = new QLPipelineRule();
      pipelineRule.setAllEvents(rule.getPipelineRule().isAllEvents());
      pipelineRule.setAllPipelines(rule.getPipelineRule().isAllPipelines());
      pipelineRule.setEvents(rule.getPipelineRule().getEvents());
      pipelineRule.setPipelineIds(rule.getPipelineRule().getPipelineIds());
      eventRule.setPipelineRule(pipelineRule);
    }

    if (rule.getWorkflowRule() != null) {
      QLWorkflowRule workflowRule = new QLWorkflowRule();
      workflowRule.setAllEvents(rule.getWorkflowRule().isAllEvents());
      workflowRule.setAllWorkflows(rule.getWorkflowRule().isAllWorkflows());
      workflowRule.setEvents(rule.getWorkflowRule().getEvents());
      workflowRule.setWorkflowIds(rule.getWorkflowRule().getWorkflowIds());
      eventRule.setWorkflowRule(workflowRule);
    }
    return eventRule;
  }
}
