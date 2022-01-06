/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.BaseYamlWithType;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
public class CgEventRule {
  public enum CgRuleType { PIPELINE, WORKFLOW, ALL }

  private CgRuleType type;
  private PipelineRule pipelineRule;
  private WorkflowRule workflowRule;

  @Data
  public static class PipelineRule {
    private boolean allEvents;
    private boolean allPipelines;
    private List<String> pipelineIds;
    private List<String> events;
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class Yaml extends BaseYamlWithType {
      private boolean allEvents;
      private boolean allPipelines;
      private List<String> pipelineIds;
      private List<String> events;
      @lombok.Builder
      public Yaml(boolean allEvents, boolean allPipelines, List<String> pipelineIds, List<String> events) {
        this.allEvents = allEvents;
        this.pipelineIds = pipelineIds;
        this.allPipelines = allPipelines;
        this.events = events;
      }
    }
  }

  @Data
  public static class WorkflowRule {
    private boolean allEvents;
    private boolean allWorkflows;
    private List<String> workflowIds;
    private List<String> events;
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class Yaml extends BaseYamlWithType {
      private boolean allEvents;
      private boolean allWorkflows;
      private List<String> workflowIds;
      private List<String> events;
      @lombok.Builder
      public Yaml(boolean allEvents, boolean allWorkflows, List<String> workflowIds, List<String> events) {
        this.allEvents = allEvents;
        this.workflowIds = workflowIds;
        this.allWorkflows = allWorkflows;
        this.events = events;
      }
    }
  }
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class Yaml extends BaseYamlWithType {
    private CgRuleType ruleType;
    private PipelineRule.Yaml pipelineRule;
    private WorkflowRule.Yaml workflowRule;
    @lombok.Builder
    public Yaml(CgRuleType cgRuleType, PipelineRule.Yaml pipelineRule, WorkflowRule.Yaml workflowRule) {
      this.workflowRule = workflowRule;
      this.pipelineRule = pipelineRule;
      this.ruleType = cgRuleType;
    }
  }
}
