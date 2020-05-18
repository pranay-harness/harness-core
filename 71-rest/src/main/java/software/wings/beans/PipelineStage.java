package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.Lists;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.NotSaved;
import software.wings.resources.ValidSkipAssert;
import software.wings.yaml.BaseYamlWithType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * Created by anubhaw on 11/17/16.
 */
@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStage {
  private String name;
  private boolean parallel;
  @Valid private List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
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
    // Remove this once UI moves away from it
    @NotSaved private boolean disable;
    @ValidSkipAssert private String disableAssertion;

    private transient boolean valid = true;
    private transient String validationMessage;

    public boolean checkDisableAssertion() {
      return disableAssertion != null && disableAssertion.equals("true");
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYamlWithType {
    private String name;
    private String stageName;
    private SkipCondition skipCondition;
    private boolean parallel;
    private String workflowName;
    private List<WorkflowVariable> workflowVariables = Lists.newArrayList();
    private Map<String, Object> properties = new HashMap<>();

    @Builder
    public Yaml(String type, String name, String stageName, boolean parallel, String workflowName,
        List<WorkflowVariable> workflowVariables, Map<String, Object> properties, SkipCondition skipCondition) {
      super(type);
      this.name = name;
      this.stageName = stageName;
      this.parallel = parallel;
      this.workflowName = workflowName;
      this.workflowVariables = workflowVariables;
      this.properties = properties;
      this.skipCondition = skipCondition;
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
