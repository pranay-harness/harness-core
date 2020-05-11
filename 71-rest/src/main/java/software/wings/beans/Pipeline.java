/**
 *
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.WorkflowType.PIPELINE;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.validator.EntityName;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.NameAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.entityinterface.KeywordsAware;
import software.wings.beans.entityinterface.TagAware;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

/**
 * The Class Pipeline.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "PipelineKeys")
@Entity(value = "pipelines", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Pipeline extends Base implements KeywordsAware, NameAccess, TagAware, AccountAccess, ApplicationAccess {
  public static final String NAME_KEY = "name";
  public static final String DESCRIPTION_KEY = "description";

  @NotNull @EntityName private String name;
  private String description;
  private List<PipelineStage> pipelineStages = new ArrayList<>();
  private Map<String, Long> stateEtaMap = new HashMap<>();
  @Transient private List<Service> services = new ArrayList<>();
  @Transient private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
  @Transient private boolean valid = true;
  @Transient private String validationMessage;
  @Transient private boolean templatized;
  private transient boolean hasSshInfraMapping;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();
  private transient List<Variable> pipelineVariables = new ArrayList<>();
  private transient List<String> envIds = new ArrayList<>();
  private transient List<String> workflowIds = new ArrayList<>();
  private transient boolean envParameterized;
  private transient List<DeploymentType> deploymentTypes = new ArrayList<>();
  private transient List<EnvSummary> envSummaries = new ArrayList<>();
  private transient boolean hasBuildWorkflow;
  private transient List<String> infraMappingIds = new ArrayList<>();
  private transient List<String> infraDefinitionIds = new ArrayList<>();
  @SchemaIgnore private Set<String> keywords;
  @Indexed private String accountId;
  private boolean sample;
  private transient List<HarnessTagLink> tagLinks;

  @Builder
  public Pipeline(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String name, String description, List<PipelineStage> pipelineStages,
      Map<String, Long> stateEtaMap, List<Service> services, List<WorkflowExecution> workflowExecutions,
      List<FailureStrategy> failureStrategies, String accountId, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.description = description;
    this.pipelineStages = (pipelineStages == null) ? new ArrayList<>() : pipelineStages;
    this.stateEtaMap = (stateEtaMap == null) ? new HashMap<>() : stateEtaMap;
    this.services = services;
    this.workflowExecutions = workflowExecutions;
    this.failureStrategies = (failureStrategies == null) ? new ArrayList<>() : failureStrategies;
    this.accountId = accountId;
    this.sample = sample;
  }

  public Pipeline cloneInternal() {
    return Pipeline.builder()
        .appId(appId)
        .accountId(accountId)
        .name(name)
        .description(description)
        .pipelineStages(pipelineStages)
        .failureStrategies(failureStrategies)
        .stateEtaMap(stateEtaMap)
        .build();
  }

  @Nonnull
  public List<String> getWorkflowIds() {
    return CollectionUtils.emptyIfNull(workflowIds);
  }

  @Override
  public Set<String> generateKeywords() {
    Set<String> keywords = KeywordsAware.super.generateKeywords();
    keywords.addAll(asList(name, description, PIPELINE.name()));
    return keywords;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
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

  @UtilityClass
  public static final class PipelineKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}
