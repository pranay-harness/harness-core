package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseYamlWithType;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PipelineStageYaml extends BaseYamlWithType {
  private String name;
  private String stageName;
  private SkipCondition skipCondition;
  private boolean parallel;
  private String workflowName;
  private List<WorkflowVariable> workflowVariables = Lists.newArrayList();
  private Map<String, Object> properties = new HashMap<>();
  private RuntimeInputsConfigYaml runtimeInputs;

  @Builder
  public PipelineStageYaml(String type, String name, String stageName, boolean parallel, String workflowName,
      List<WorkflowVariable> workflowVariables, Map<String, Object> properties, SkipCondition skipCondition,
      RuntimeInputsConfigYaml runtimeInputs) {
    super(type);
    this.name = name;
    this.stageName = stageName;
    this.parallel = parallel;
    this.workflowName = workflowName;
    this.workflowVariables = workflowVariables;
    this.properties = properties;
    this.skipCondition = skipCondition;
    this.runtimeInputs = runtimeInputs;
  }
}
