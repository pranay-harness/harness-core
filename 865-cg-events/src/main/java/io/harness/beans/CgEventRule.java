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
    @lombok.Builder
    public Yaml(CgRuleType cgRuleType, PipelineRule.Yaml pipelineRule) {
      this.pipelineRule = pipelineRule;
      this.ruleType = cgRuleType;
    }
  }
}
