package software.wings.api;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.context.ContextElementType;
import io.harness.expression.ExpressionEvaluator;
import software.wings.beans.EntityType;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

public class ServiceArtifactVariableElement implements ContextElement {
  private String uuid;
  private String name;
  private EntityType entityType;
  private String entityId;
  private String serviceId;
  private String artifactVariableName;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.ARTIFACT_VARIABLE;
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

  public EntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getArtifactVariableName() {
    return isBlank(artifactVariableName) ? ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME : artifactVariableName;
  }

  public void setArtifactVariableName(String artifactVariableName) {
    this.artifactVariableName = artifactVariableName;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class ServiceArtifactVariableElementBuilder {
    private String uuid;
    private String name;
    private EntityType entityType;
    private String entityId;
    private String serviceId;
    private String artifactVariableName;

    private ServiceArtifactVariableElementBuilder() {}

    public static ServiceArtifactVariableElementBuilder aServiceArtifactVariableElement() {
      return new ServiceArtifactVariableElementBuilder();
    }

    public ServiceArtifactVariableElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ServiceArtifactVariableElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ServiceArtifactVariableElementBuilder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public ServiceArtifactVariableElementBuilder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public ServiceArtifactVariableElementBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public ServiceArtifactVariableElementBuilder withArtifactVariableName(String artifactVariableName) {
      this.artifactVariableName = artifactVariableName;
      return this;
    }

    public ServiceArtifactVariableElement build() {
      ServiceArtifactVariableElement serviceArtifactVariableElement = new ServiceArtifactVariableElement();
      serviceArtifactVariableElement.setUuid(uuid);
      serviceArtifactVariableElement.setName(name);
      serviceArtifactVariableElement.setEntityType(entityType);
      serviceArtifactVariableElement.setEntityId(entityId);
      serviceArtifactVariableElement.setServiceId(serviceId);
      serviceArtifactVariableElement.setArtifactVariableName(artifactVariableName);
      return serviceArtifactVariableElement;
    }
  }
}
