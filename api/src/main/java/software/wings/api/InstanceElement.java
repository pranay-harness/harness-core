/**
 *
 */

package software.wings.api;

import static software.wings.api.InstanceElement.Builder.anInstanceElement;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class InstanceElement.
 *
 * @author Rishi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceElement implements ContextElement {
  private String uuid;
  private String displayName;
  private String hostName;
  private String dockerId;
  private HostElement host;
  private ServiceTemplateElement serviceTemplateElement;
  private String podName;
  private String workloadName;

  @Override
  public String getName() {
    return displayName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.INSTANCE;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(INSTANCE, this);
    if (host != null) {
      map.putAll(host.paramMap(context));
    }
    if (serviceTemplateElement != null) {
      map.putAll(serviceTemplateElement.paramMap(context));
    }
    return map;
  }

  public String getUuid() {
    return uuid;
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
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getDockerId() {
    return dockerId;
  }

  public void setDockerId(String dockerId) {
    this.dockerId = dockerId;
  }

  public String getPodName() {
    return podName;
  }

  public void setPodName(String podName) {
    this.podName = podName;
  }

  public String getWorkloadName() {
    return workloadName;
  }

  public void setWorkloadName(String workloadName) {
    this.workloadName = workloadName;
  }

  /**
   * Gets host element.
   *
   * @return the host element
   */
  public HostElement getHost() {
    return host;
  }

  /**
   * Sets host element.
   *
   * @param hostElement the host element
   */
  public void setHost(HostElement hostElement) {
    this.host = hostElement;
  }

  /**
   * Gets service template element.
   *
   * @return the service template element
   */
  public ServiceTemplateElement getServiceTemplateElement() {
    return serviceTemplateElement;
  }

  /**
   * Sets service template element.
   *
   * @param serviceTemplateElement the service template element
   */
  public void setServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
    this.serviceTemplateElement = serviceTemplateElement;
  }

  @Override
  public ContextElement cloneMin() {
    InstanceElement instanceElement =
        anInstanceElement().withUuid(uuid).withDisplayName(displayName).withHostName(hostName).build();
    if (host != null) {
      instanceElement.setHost((HostElement) host.cloneMin());
    }
    return instanceElement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InstanceElement that = (InstanceElement) o;
    return Objects.equal(getUuid(), that.getUuid()) && Objects.equal(getDisplayName(), that.getDisplayName())
        && Objects.equal(getHostName(), that.getHostName()) && Objects.equal(getDockerId(), that.getDockerId())
        && Objects.equal(getHost(), that.getHost())
        && Objects.equal(getServiceTemplateElement(), that.getServiceTemplateElement())
        && Objects.equal(getPodName(), that.getPodName()) && Objects.equal(getWorkloadName(), that.getWorkloadName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getUuid(), getDisplayName(), getHostName(), getDockerId(), getHost(),
        getServiceTemplateElement(), getPodName(), getWorkloadName());
  }

  @Override
  public String toString() {
    return "InstanceElement{"
        + "uuid='" + uuid + '\'' + ", displayName='" + displayName + '\'' + ", hostName='" + hostName + '\''
        + ", hostElement=" + host + ", serviceTemplateElement=" + serviceTemplateElement + ", podName=" + podName
        + ", workloadName=" + workloadName + '}';
  }

  public static final class Builder {
    private String uuid;
    private String displayName;
    private String hostName;
    private String dockerId;
    private HostElement host;
    private ServiceTemplateElement serviceTemplateElement;
    private String podName;
    private String workloadName;

    private Builder() {}

    public static Builder anInstanceElement() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withDockerId(String dockerId) {
      this.dockerId = dockerId;
      return this;
    }

    public Builder withHost(HostElement host) {
      this.host = host;
      return this;
    }

    public Builder withServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
      this.serviceTemplateElement = serviceTemplateElement;
      return this;
    }

    public Builder withPodName(String podName) {
      this.podName = podName;
      return this;
    }

    public Builder withWorkloadName(String workloadName) {
      this.workloadName = workloadName;
      return this;
    }

    public Builder but() {
      return anInstanceElement()
          .withUuid(uuid)
          .withDisplayName(displayName)
          .withHostName(hostName)
          .withDockerId(dockerId)
          .withHost(host)
          .withServiceTemplateElement(serviceTemplateElement)
          .withPodName(podName)
          .withWorkloadName(workloadName);
    }

    public InstanceElement build() {
      InstanceElement instanceElement = new InstanceElement();
      instanceElement.setUuid(uuid);
      instanceElement.setDisplayName(displayName);
      instanceElement.setHostName(hostName);
      instanceElement.setDockerId(dockerId);
      instanceElement.setHost(host);
      instanceElement.setServiceTemplateElement(serviceTemplateElement);
      instanceElement.setPodName(podName);
      instanceElement.setWorkloadName(workloadName);
      return instanceElement;
    }
  }
}
