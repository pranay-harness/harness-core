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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineStage {
  private String name;
  private boolean parallel;
  private List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
  private boolean valid = true;
  private String validationMessage;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class PipelineStageElement {
    private String uuid;
    private String name;
    private String type;
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> workflowVariables = new HashMap<>();

    private boolean valid = true;
    private String validationMessage;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYamlWithType {
    private String uuid;
    private String name;
    private boolean parallel;
    private String workflowName;
    private List<NameValuePair.Yaml> workflowVariables = Lists.newArrayList();

    @Builder
    public Yaml(String type, String uuid, String name, boolean parallel, String workflowName,
        List<NameValuePair.Yaml> workflowVariables) {
      super(type);
      this.uuid = uuid;
      this.name = name;
      this.parallel = parallel;
      this.workflowName = workflowName;
      this.workflowVariables = workflowVariables;
    }
  }
}
