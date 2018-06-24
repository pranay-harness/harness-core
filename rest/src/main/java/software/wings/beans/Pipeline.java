/**
 *
 */

package software.wings.beans;

import static java.util.Arrays.asList;
import static software.wings.beans.WorkflowType.PIPELINE;

import io.harness.data.validator.EntityName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * The Class Pipeline.
 *
 * @author Rishi
 */
@Entity(value = "pipelines", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline extends Base {
  @NotNull @EntityName private String name;
  private String description;
  @NotNull private List<PipelineStage> pipelineStages = new ArrayList<>();
  private Map<String, Long> stateEtaMap = new HashMap<>();
  @Transient private List<Service> services = new ArrayList<>();
  @Transient private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
  @Transient private boolean valid = true;
  @Transient private String validationMessage;
  @Transient private boolean templatized;
  private transient boolean hasSshInfraMapping;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();
  private transient List<Variable> pipelineVariables = new ArrayList<>();

  @Builder
  public Pipeline(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String name, String description,
      List<PipelineStage> pipelineStages, Map<String, Long> stateEtaMap, List<Service> services,
      List<WorkflowExecution> workflowExecutions, List<FailureStrategy> failureStrategies) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.name = name;
    this.description = description;
    this.pipelineStages = (pipelineStages == null) ? new ArrayList<>() : pipelineStages;
    this.stateEtaMap = (stateEtaMap == null) ? new HashMap<>() : stateEtaMap;
    this.services = services;
    this.workflowExecutions = workflowExecutions;
    this.failureStrategies = (failureStrategies == null) ? new ArrayList<>() : failureStrategies;
  }

  public Pipeline cloneInternal() {
    return Pipeline.builder()
        .appId(appId)
        .name(name)
        .description(description)
        .pipelineStages(pipelineStages)
        .failureStrategies(failureStrategies)
        .stateEtaMap(stateEtaMap)
        .build();
  }

  @Override
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description, PIPELINE));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    private List<PipelineStage.Yaml> pipelineStages = new ArrayList<>();
    private List<FailureStrategy.Yaml> failureStrategies;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, List<PipelineStage.Yaml> pipelineStages) {
      super(EntityType.PIPELINE.name(), harnessApiVersion);
      this.description = description;
      this.pipelineStages = pipelineStages;
    }
  }
}
