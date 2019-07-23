package software.wings.beans;

import com.google.common.collect.Lists;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 11/17/16.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStage {
  private String name;
  private boolean parallel;
  private List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
  private transient boolean valid = true;
  private transient String validationMessage;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PipelineStageElement {
    private String uuid;
    private String name;
    private String type;
    private int parallelIndex;
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> workflowVariables = new HashMap<>();
    private boolean disable;

    private transient boolean valid = true;
    private transient String validationMessage;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYamlWithType {
    private String name;
    private boolean parallel;
    private String workflowName;
    private List<WorkflowVariable> workflowVariables = Lists.newArrayList();
    private Map<String, Object> properties = new HashMap<>();

    @Builder
    public Yaml(String type, String name, boolean parallel, String workflowName,
        List<WorkflowVariable> workflowVariables, Map<String, Object> properties) {
      super(type);
      this.name = name;
      this.parallel = parallel;
      this.workflowName = workflowName;
      this.workflowVariables = workflowVariables;
      this.properties = properties;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class WorkflowVariable extends NameValuePair.AbstractYaml {
    String entityType;

    @Builder
    public WorkflowVariable(
        String entityType, String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls) {
      super(name, value, valueType, allowedValueYamls);
      this.entityType = entityType;
    }
  }
}
