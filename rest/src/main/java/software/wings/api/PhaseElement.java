/**
 *
 */

package software.wings.api;

import static io.harness.govern.Switch.unhandled;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NameValuePair;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class PhaseElement.
 *
 * @author Rishi
 */
public class PhaseElement implements ContextElement {
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient ArtifactService artifactService;

  private String uuid;
  private String phaseName;
  private ServiceElement serviceElement;
  private String appId;
  private String infraMappingId;
  private String deploymentType;
  private String phaseNameForRollback;
  private List<NameValuePair> variableOverrides = new ArrayList<>();
  private String rollbackArtifactId;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getName() {
    return Constants.PHASE_PARAM;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ServiceElement getServiceElement() {
    return serviceElement;
  }

  public void setServiceElement(ServiceElement serviceElement) {
    this.serviceElement = serviceElement;
  }

  public String getPhaseName() {
    return phaseName;
  }

  public void setPhaseName(String phaseName) {
    this.phaseName = phaseName;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, serviceElement);
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infrastructureMapping != null
        && ((DeploymentType.KUBERNETES.name().equals(infrastructureMapping.getDeploymentType()))
               || (DeploymentType.HELM.name().equals(infrastructureMapping.getDeploymentType())))) {
      String namespace = null;
      if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
        namespace = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      } else if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
        namespace = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
        namespace = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      } else {
        unhandled(infrastructureMapping.getInfraMappingType());
      }
      if (isBlank(namespace)) {
        namespace = "default";
      }
      Map<String, Object> namespaceMap = new HashMap<>();
      namespaceMap.put(NAMESPACE, namespace);

      Map<String, Object> kubernetesMap = new HashMap<>();
      kubernetesMap.put(KUBERNETES, namespaceMap);
      map.put(INFRA, kubernetesMap);
    }
    if (rollbackArtifactId != null) {
      Artifact artifact = artifactService.get(appId, rollbackArtifactId);
      map.put(ARTIFACT, artifact);
    }
    return map;
  }

  public String getPhaseNameForRollback() {
    return phaseNameForRollback;
  }

  public void setPhaseNameForRollback(String phaseNameForRollback) {
    this.phaseNameForRollback = phaseNameForRollback;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public String getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public List<NameValuePair> getVariableOverrides() {
    return variableOverrides;
  }

  public void setVariableOverrides(List<NameValuePair> variableOverrides) {
    this.variableOverrides = variableOverrides;
  }

  public String getRollbackArtifactId() {
    return rollbackArtifactId;
  }

  public void setRollbackArtifactId(String rollbackArtifactId) {
    this.rollbackArtifactId = rollbackArtifactId;
  }

  public static final class PhaseElementBuilder {
    private String uuid;
    private ServiceElement serviceElement;
    private String appId;
    private String infraMappingId;
    private String deploymentType;
    private String phaseNameForRollback;
    private String phaseName;
    private List<NameValuePair> variableOverrides = new ArrayList<>();
    private String rollbackArtifactId;

    private PhaseElementBuilder() {}

    public static PhaseElementBuilder aPhaseElement() {
      return new PhaseElementBuilder();
    }

    public PhaseElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public PhaseElementBuilder withServiceElement(ServiceElement serviceElement) {
      this.serviceElement = serviceElement;
      return this;
    }

    public PhaseElementBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public PhaseElementBuilder withPhaseName(String phaseName) {
      this.phaseName = phaseName;
      return this;
    }

    public PhaseElementBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public PhaseElementBuilder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhaseElementBuilder withPhaseNameForRollback(String phaseNameForRollback) {
      this.phaseNameForRollback = phaseNameForRollback;
      return this;
    }

    public PhaseElementBuilder withVariableOverrides(List<NameValuePair> variableOverrides) {
      this.variableOverrides = variableOverrides;
      return this;
    }

    public PhaseElementBuilder withRollbackArtifactId(String rollbackArtifactId) {
      this.rollbackArtifactId = rollbackArtifactId;
      return this;
    }

    public PhaseElementBuilder but() {
      return aPhaseElement()
          .withUuid(uuid)
          .withServiceElement(serviceElement)
          .withAppId(appId)
          .withInfraMappingId(infraMappingId)
          .withDeploymentType(deploymentType)
          .withPhaseNameForRollback(phaseNameForRollback)
          .withPhaseName(phaseName)
          .withVariableOverrides(variableOverrides)
          .withRollbackArtifactId(rollbackArtifactId);
    }

    public PhaseElement build() {
      PhaseElement phaseElement = new PhaseElement();
      phaseElement.setUuid(uuid);
      phaseElement.setServiceElement(serviceElement);
      phaseElement.setAppId(appId);
      phaseElement.setInfraMappingId(infraMappingId);
      phaseElement.setDeploymentType(deploymentType);
      phaseElement.setPhaseNameForRollback(phaseNameForRollback);
      phaseElement.setPhaseName(phaseName);
      phaseElement.setVariableOverrides(variableOverrides);
      phaseElement.setRollbackArtifactId(rollbackArtifactId);
      return phaseElement;
    }
  }
}
