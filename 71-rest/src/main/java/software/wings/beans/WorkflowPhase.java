package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.sm.StateType.PHASE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.DeploymentType;
import software.wings.common.Constants;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.sm.TransitionType;
import software.wings.yaml.BaseYamlWithType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.Valid;

@Data
@Builder
public class WorkflowPhase implements UuidAccess {
  @Default private String uuid = generateUuid();
  private String name;
  private String serviceId;
  private boolean daemonSet;
  private boolean statefulSet;

  private String infraMappingId;
  private String infraMappingName;

  private DeploymentType deploymentType;
  private String computeProviderId;
  private boolean provisionNodes;

  private boolean rollback;
  private String phaseNameForRollback;

  @Default private boolean valid = true;
  private String validationMessage;

  private List<TemplateExpression> templateExpressions;

  @Default @Valid private List<NameValuePair> variableOverrides = new ArrayList<>();

  @Default private List<PhaseStep> phaseSteps = new ArrayList<>();

  public GraphNode generatePhaseNode() {
    return GraphNode.builder()
        .id(uuid)
        .name(name)
        .type(PHASE.name())
        .rollback(rollback)
        .properties(NullSafeImmutableMap.builder()
                        .putIfNotNull("serviceId", serviceId)
                        .putIfNotNull("deploymentType", deploymentType)
                        .putIfNotNull("computeProviderId", computeProviderId)
                        .putIfNotNull("infraMappingName", infraMappingName)
                        .putIfNotNull("infraMappingId", infraMappingId)
                        .putIfNotNull(Constants.SUB_WORKFLOW_ID, uuid)
                        .putIfNotNull("phaseNameForRollback", phaseNameForRollback)
                        .build())
        .templateExpressions(templateExpressions)
        .variableOverrides(variableOverrides)
        .build();
  }

  public Map<String, Object> params() {
    Map<String, Object> params = new HashMap<>();
    params.put("serviceId", serviceId);
    params.put("computeProviderId", computeProviderId);
    params.put("infraMappingName", infraMappingName);
    params.put("infraMappingId", infraMappingId);
    params.put("deploymentType", deploymentType);
    return params;
  }

  public Map<String, Graph> generateSubworkflows() {
    Map<String, Graph> graphs = new HashMap<>();
    Graph.Builder graphBuilder = aGraph().withGraphName(name);

    String id1 = null;
    String id2;
    GraphNode node;
    for (PhaseStep phaseStep : phaseSteps) {
      id2 = phaseStep.getUuid();
      node = phaseStep.generatePhaseStepNode();
      graphBuilder.addNodes(node);
      if (id1 == null) {
        node.setOrigin(true);
      } else {
        graphBuilder.addLinks(
            aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build());
      }
      id1 = id2;
      Graph stepsGraph = phaseStep.generateSubworkflow(deploymentType);
      graphs.put(phaseStep.getUuid(), stepsGraph);
    }

    graphs.put(uuid, graphBuilder.build());
    return graphs;
  }

  public boolean validate() {
    valid = true;
    validationMessage = null;
    if (phaseSteps != null) {
      List<String> invalidChildren =
          phaseSteps.stream().filter(phaseStep -> !phaseStep.validate()).map(PhaseStep::getName).collect(toList());
      if (isNotEmpty(invalidChildren)) {
        valid = false;
        validationMessage = format(Constants.PHASE_VALIDATION_MESSAGE, invalidChildren.toString());
      }
    }

    return valid;
  }

  public WorkflowPhase cloneInternal() {
    WorkflowPhase clonedWorkflowPhase = builder()
                                            .uuid(generateUuid())
                                            .serviceId(getServiceId())
                                            .infraMappingId(getInfraMappingId())
                                            .infraMappingName(getInfraMappingName())
                                            .computeProviderId(getComputeProviderId())
                                            .deploymentType(getDeploymentType())
                                            .rollback(isRollback())
                                            .phaseNameForRollback(getPhaseNameForRollback())
                                            .valid(isValid())
                                            .validationMessage(getValidationMessage())
                                            .templateExpressions(getTemplateExpressions())
                                            .daemonSet(isDaemonSet())
                                            .statefulSet(isStatefulSet())
                                            .build();
    List<PhaseStep> phaseSteps = getPhaseSteps();
    List<PhaseStep> clonedPhaseSteps = new ArrayList<>();
    if (phaseSteps != null) {
      clonedPhaseSteps =
          phaseSteps.stream().map(PhaseStep::cloneIntenal).filter(Objects::nonNull).collect(Collectors.toList());
    }
    clonedWorkflowPhase.setPhaseSteps(clonedPhaseSteps);
    return clonedWorkflowPhase;
  }

  @JsonIgnore
  public boolean checkServiceTemplatized() {
    return checkFieldTemplatized("serviceId");
  }

  @JsonIgnore
  public boolean checkInfraTemplatized() {
    return checkFieldTemplatized("infraMappingId");
  }

  public String fetchServiceTemplatizedName() {
    TemplateExpression serviceTemplateExpression = fetchServiceTemplateExpression();
    return WorkflowServiceTemplateHelper.getName(serviceTemplateExpression.getExpression(), EntityType.SERVICE);
  }

  public String fetchInfraMappingTemplatizedName() {
    TemplateExpression infraTemplateExpression = fetchInfraMappingTemplateExpression();
    return WorkflowServiceTemplateHelper.getName(
        infraTemplateExpression.getExpression(), EntityType.INFRASTRUCTURE_MAPPING);
  }

  private boolean checkFieldTemplatized(String fieldName) {
    return templateExpressions != null
        && templateExpressions.stream().anyMatch(
               templateExpression -> templateExpression.getFieldName().equals(fieldName));
  }

  private TemplateExpression fetchServiceTemplateExpression() {
    return templateExpressions.stream()
        .filter(templateExpression -> templateExpression.getFieldName().equals("serviceId"))
        .findFirst()
        .orElse(null);
  }

  private TemplateExpression fetchInfraMappingTemplateExpression() {
    return templateExpressions.stream()
        .filter(templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))
        .findFirst()
        .orElse(null);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYamlWithType {
    private String name;
    private String infraMappingName;
    private String serviceName;
    private String computeProviderName;
    private boolean provisionNodes;
    private boolean daemonSet;
    private boolean statefulSet;
    private String phaseNameForRollback;
    private List<TemplateExpression.Yaml> templateExpressions;
    private List<PhaseStep.Yaml> phaseSteps = new ArrayList<>();
    //  private DeploymentType deploymentType;

    @Builder
    public Yaml(String type, String name, String infraMappingName, String serviceName, String computeProviderName,
        boolean provisionNodes, String phaseNameForRollback, List<TemplateExpression.Yaml> templateExpressions,
        List<PhaseStep.Yaml> phaseSteps, boolean daemonSet, boolean statefulSet) {
      super(type);
      this.name = name;
      this.infraMappingName = infraMappingName;
      this.serviceName = serviceName;
      this.computeProviderName = computeProviderName;
      this.provisionNodes = provisionNodes;
      this.phaseNameForRollback = phaseNameForRollback;
      this.templateExpressions = templateExpressions;
      this.phaseSteps = phaseSteps;
      this.daemonSet = daemonSet;
      this.statefulSet = statefulSet;
    }
  }
}
