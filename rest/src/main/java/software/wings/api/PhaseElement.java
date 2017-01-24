/**
 *
 */

package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class ServiceElement.
 *
 * @author Rishi
 */
public class PhaseElement implements ContextElement {
  private String uuid;
  private ServiceElement serviceElement;
  private String computeProviderId;
  private DeploymentType deploymentType;

  // Only relevant for custom kubernetes environment
  private String deploymentMasterId;

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

  public String getComputeProviderId() {
    return computeProviderId;
  }

  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getDeploymentMasterId() {
    return deploymentMasterId;
  }

  public void setDeploymentMasterId(String deploymentMasterId) {
    this.deploymentMasterId = deploymentMasterId;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, serviceElement);
    return map;
  }

  public static final class PhaseElementBuilder {
    private String uuid;
    private ServiceElement serviceElement;
    private String computeProviderId;
    private DeploymentType deploymentType;
    // Only relevant for custom kubernetes environment
    private String deploymentMasterId;

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

    public PhaseElementBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public PhaseElementBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhaseElementBuilder withDeploymentMasterId(String deploymentMasterId) {
      this.deploymentMasterId = deploymentMasterId;
      return this;
    }

    public PhaseElement build() {
      PhaseElement phaseElement = new PhaseElement();
      phaseElement.setUuid(uuid);
      phaseElement.setServiceElement(serviceElement);
      phaseElement.setComputeProviderId(computeProviderId);
      phaseElement.setDeploymentType(deploymentType);
      phaseElement.setDeploymentMasterId(deploymentMasterId);
      return phaseElement;
    }
  }
}
