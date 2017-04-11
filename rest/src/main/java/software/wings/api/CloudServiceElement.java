package software.wings.api;

import static software.wings.sm.ContextElementType.CLOUD_SERVICE;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * Created by rishi on 2/10/17.
 */
public class CloudServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private String oldName;
  private String clusterName;

  @Override
  public ContextElementType getElementType() {
    return CLOUD_SERVICE;
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

  public String getOldName() {
    return oldName;
  }

  public void setOldName(String oldName) {
    this.oldName = oldName;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public static final class CloudServiceElementBuilder {
    private String uuid;
    private String name;
    private String oldName;
    private String clusterName;

    private CloudServiceElementBuilder() {}

    public static CloudServiceElementBuilder aCloudServiceElement() {
      return new CloudServiceElementBuilder();
    }

    public CloudServiceElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public CloudServiceElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public CloudServiceElementBuilder withOldName(String oldName) {
      this.oldName = oldName;
      return this;
    }

    public CloudServiceElementBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public CloudServiceElementBuilder but() {
      return aCloudServiceElement().withUuid(uuid).withName(name).withOldName(oldName).withClusterName(clusterName);
    }

    public CloudServiceElement build() {
      CloudServiceElement cloudServiceElement = new CloudServiceElement();
      cloudServiceElement.setUuid(uuid);
      cloudServiceElement.setName(name);
      cloudServiceElement.setOldName(oldName);
      cloudServiceElement.setClusterName(clusterName);
      return cloudServiceElement;
    }
  }
}
