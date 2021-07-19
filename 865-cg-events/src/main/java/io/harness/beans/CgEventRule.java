package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;

@OwnedBy(CDC)
@Data
public class CgEventRule {
  public static final List<String> WORKFLOW_EVENTS = Lists.newArrayList("workflow_start", "workflow_end");
  public static final List<String> PIPELINE_EVENTS = Lists.newArrayList("pipeline_start", "pipeline_end");

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
  }

  @Data
  public static class WorkflowRule {
    private boolean allEvents;
    private boolean allWorkflows;
    private List<String> workflowIds;
    private List<String> events;
  }
}
