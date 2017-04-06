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
  private String infraMappingId;
  private String deploymentType;
  private String phaseNameForRollback;

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

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets service element.
   *
   * @return the service element
   */
  public ServiceElement getServiceElement() {
    return serviceElement;
  }

  /**
   * Sets service element.
   *
   * @param serviceElement the service element
   */
  public void setServiceElement(ServiceElement serviceElement) {
    this.serviceElement = serviceElement;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, serviceElement);
    return map;
  }

  public String getPhaseNameForRollback() {
    return phaseNameForRollback;
  }

  public void setPhaseNameForRollback(String phaseNameForRollback) {
    this.phaseNameForRollback = phaseNameForRollback;
  }

  /**
   * Gets infr mapping id.
   *
   * @return the infr mapping id
   */
  public String getInfraMappingId() {
    return infraMappingId;
  }

  /**
   * Sets infr mapping id.
   *
   * @param infraMappingId the infr mapping id
   */
  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  /**
   * Gets deployment type.
   *
   * @return the deployment type
   */
  public String getDeploymentType() {
    return deploymentType;
  }

  /**
   * Sets deployment type.
   *
   * @param deploymentType the deployment type
   */
  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  /**
   * The type Phase element builder.
   */
  public static final class PhaseElementBuilder {
    private String uuid;
    private ServiceElement serviceElement;
    private String infraMappingId;
    private String deploymentType;
    private String phaseNameForRollback;

    private PhaseElementBuilder() {}

    /**
     * A phase element phase element builder.
     *
     * @return the phase element builder
     */
    public static PhaseElementBuilder aPhaseElement() {
      return new PhaseElementBuilder();
    }

    /**
     * With uuid phase element builder.
     *
     * @param uuid the uuid
     * @return the phase element builder
     */
    public PhaseElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With service element phase element builder.
     *
     * @param serviceElement the service element
     * @return the phase element builder
     */
    public PhaseElementBuilder withServiceElement(ServiceElement serviceElement) {
      this.serviceElement = serviceElement;
      return this;
    }

    /**
     * With infra mapping id phase element builder.
     *
     * @param infraMappingId the infra mapping id
     * @return the phase element builder
     */
    public PhaseElementBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public PhaseElementBuilder withPhaseNameForRollback(String phaseNameForRollback) {
      this.phaseNameForRollback = phaseNameForRollback;
      return this;
    }

    /**
     * With deployment type phase element builder.
     *
     * @param deploymentType the deployment type
     * @return the phase element builder
     */
    public PhaseElementBuilder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    /**
     * But phase element builder.
     *
     * @return the phase element builder
     */
    public PhaseElementBuilder but() {
      return aPhaseElement()
          .withUuid(uuid)
          .withServiceElement(serviceElement)
          .withInfraMappingId(infraMappingId)
          .withDeploymentType(deploymentType)
          .withPhaseNameForRollback(phaseNameForRollback);
    }

    /**
     * Build phase element.
     *
     * @return the phase element
     */
    public PhaseElement build() {
      PhaseElement phaseElement = new PhaseElement();
      phaseElement.setUuid(uuid);
      phaseElement.setServiceElement(serviceElement);
      phaseElement.setInfraMappingId(infraMappingId);
      phaseElement.setDeploymentType(deploymentType);
      phaseElement.setPhaseNameForRollback(phaseNameForRollback);
      return phaseElement;
    }
  }
}
