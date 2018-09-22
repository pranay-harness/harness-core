package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

public class ServiceArtifactElement implements ContextElement {
  private String uuid;
  private String name;
  private List<String> serviceIds;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.ARTIFACT;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(List<String> serviceIds) {
    this.serviceIds = serviceIds;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class ServiceArtifactElementBuilder {
    private String uuid;
    private String name;
    private List<String> serviceIds;

    private ServiceArtifactElementBuilder() {}

    public static ServiceArtifactElementBuilder aServiceArtifactElement() {
      return new ServiceArtifactElementBuilder();
    }

    public ServiceArtifactElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ServiceArtifactElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ServiceArtifactElementBuilder withServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    public ServiceArtifactElement build() {
      ServiceArtifactElement serviceArtifactElement = new ServiceArtifactElement();
      serviceArtifactElement.setUuid(uuid);
      serviceArtifactElement.setName(name);
      serviceArtifactElement.setServiceIds(serviceIds);
      return serviceArtifactElement;
    }
  }
}
