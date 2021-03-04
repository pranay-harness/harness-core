package software.wings.beans;

import software.wings.yaml.BaseYamlWithType;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class WorkflowPhaseYaml extends BaseYamlWithType {
  private String name;
  private String infraMappingName;
  private String infraDefinitionName;
  private String serviceName;
  private String computeProviderName;
  private boolean provisionNodes;
  private boolean daemonSet;
  private boolean statefulSet;
  private String phaseNameForRollback;
  private List<TemplateExpressionYaml> templateExpressions;
  private List<PhaseStep.Yaml> phaseSteps = new ArrayList<>();
  private List<NameValuePair> serviceVariableOverrides = new ArrayList<>();
  //  private DeploymentType deploymentType;

  @lombok.Builder
  public WorkflowPhaseYaml(String type, String name, String infraMappingName, String infraDefinitionName,
      String serviceName, String computeProviderName, boolean provisionNodes, String phaseNameForRollback,
      List<TemplateExpressionYaml> templateExpressions, List<PhaseStep.Yaml> phaseSteps, boolean daemonSet,
      boolean statefulSet, List<NameValuePair> serviceVariableOverrides) {
    super(type);
    this.name = name;
    this.infraMappingName = infraMappingName;
    this.infraDefinitionName = infraDefinitionName;
    this.serviceName = serviceName;
    this.computeProviderName = computeProviderName;
    this.provisionNodes = provisionNodes;
    this.phaseNameForRollback = phaseNameForRollback;
    this.templateExpressions = templateExpressions;
    this.phaseSteps = phaseSteps;
    this.daemonSet = daemonSet;
    this.statefulSet = statefulSet;
    this.serviceVariableOverrides = serviceVariableOverrides;
  }
}
