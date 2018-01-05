package software.wings.beans;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.sm.StateType.PHASE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Embedded;
import software.wings.api.DeploymentType;
import software.wings.beans.Graph.Builder;
import software.wings.beans.Graph.Node;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.TransitionType;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by rishi on 12/21/16.
 */
@Data
@lombok.Builder(toBuilder = true)
public class WorkflowPhase implements UuidAware {
  @lombok.Builder.Default private String uuid = UUIDGenerator.getUuid();
  private String name;
  private String serviceId;

  private String infraMappingId;
  private String infraMappingName;

  private DeploymentType deploymentType;
  private String computeProviderId;
  private boolean provisionNodes;

  private boolean rollback;
  private String phaseNameForRollback;

  @lombok.Builder.Default private boolean valid = true;
  private String validationMessage;

  private List<TemplateExpression> templateExpressions;

  private List<NameValuePair> variableOverrides = new ArrayList<>();

  @Embedded private List<PhaseStep> phaseSteps = new ArrayList<>();

  public void addPhaseStep(PhaseStep phaseStep) {
    if (phaseSteps == null) {
      phaseSteps = new ArrayList<>();
    }
    phaseSteps.add(phaseStep);
  }

  public Node generatePhaseNode() {
    return aNode()
        .withId(uuid)
        .withName(name)
        .withType(PHASE.name())
        .addProperty("serviceId", serviceId)
        .withRollback(rollback)
        .addProperty("deploymentType", deploymentType)
        .addProperty("computeProviderId", computeProviderId)
        .addProperty("infraMappingName", infraMappingName)
        .addProperty("infraMappingId", infraMappingId)
        .addProperty(Constants.SUB_WORKFLOW_ID, uuid)
        .addProperty("phaseNameForRollback", phaseNameForRollback)
        .withTemplateExpressions(templateExpressions)
        .withVariableOverrides(variableOverrides)
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

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public Map<String, Graph> generateSubworkflows() {
    Map<String, Graph> graphs = new HashMap<>();
    Builder graphBuilder = aGraph().withGraphName(name);

    String id1 = null;
    String id2;
    Node node;
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
      List<String> invalidChildren = phaseSteps.stream()
                                         .filter(phaseStep -> !phaseStep.validate())
                                         .map(PhaseStep::getName)
                                         .collect(Collectors.toList());
      if (invalidChildren != null && !invalidChildren.isEmpty()) {
        valid = false;
        validationMessage = String.format(Constants.PHASE_VALIDATION_MESSAGE, invalidChildren.toString());
      }
    }
    return valid;
  }

  public WorkflowPhase clone() {
    List<PhaseStep> phaseSteps = getPhaseSteps();
    List<PhaseStep> clonedPhaseSteps = new ArrayList<>();
    if (phaseSteps != null) {
      for (PhaseStep phaseStep : phaseSteps) {
        PhaseStep phaseStepClone = phaseStep.clone();
        if (phaseStepClone != null) {
          clonedPhaseSteps.add(phaseStepClone);
        }
      }
    }
    return this.toBuilder().uuid(UUIDGenerator.getUuid()).phaseSteps(clonedPhaseSteps).build();
  }

  @JsonIgnore
  public boolean checkServiceTemplatized() {
    return checkFieldTemplatized("serviceId");
  }

  @JsonIgnore
  public boolean checkInfraTemplatized() {
    return checkFieldTemplatized("infraMappingId");
  }

  private boolean checkFieldTemplatized(String fieldName) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals(fieldName));
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String name;
    private String infraMappingName;
    private String serviceName;
    private String computeProviderName;
    private boolean provisionNodes;
    private String phaseNameForRollback;
    private List<TemplateExpression.Yaml> templateExpressions;
    private List<PhaseStep.Yaml> phaseSteps = new ArrayList<>();
    //  private DeploymentType deploymentType;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String name, String infraMappingName, String serviceName,
        String computeProviderName, boolean provisionNodes, String phaseNameForRollback,
        List<TemplateExpression.Yaml> templateExpressions, List<PhaseStep.Yaml> phaseSteps) {
      super(type, harnessApiVersion);
      this.name = name;
      this.infraMappingName = infraMappingName;
      this.serviceName = serviceName;
      this.computeProviderName = computeProviderName;
      this.provisionNodes = provisionNodes;
      this.phaseNameForRollback = phaseNameForRollback;
      this.templateExpressions = templateExpressions;
      this.phaseSteps = phaseSteps;
    }
  }
}
