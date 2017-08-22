package software.wings.beans;

import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.RELATED_FIELD;
import static software.wings.common.Constants.STATE_TYPE;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.sm.StateType;
import software.wings.utils.ArtifactType;

import java.beans.Transient;
import java.util.Map;

/**
 * Created by rishi on 12/21/16.
 */
public class Variable {
  private String name;
  private String description;
  private boolean mandatory;
  private String value;
  private boolean fixed;

  private Map<String, Object> metadata = Maps.newHashMap();

  private VariableType type = VariableType.TEXT;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public VariableType getType() {
    return type;
  }

  public void setType(VariableType type) {
    this.type = type;
  }

  public boolean isFixed() {
    return fixed;
  }

  public void setFixed(boolean fixed) {
    this.fixed = fixed;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @Transient
  @JsonIgnore
  public EntityType getEntityType() {
    return (EntityType) metadata.get(ENTITY_TYPE);
  }

  @Transient
  @JsonIgnore
  public ArtifactType getArtifactType() {
    return (ArtifactType) metadata.get(ARTIFACT_TYPE);
  }

  @Transient
  @JsonIgnore
  public String getRelatedField() {
    return (String) metadata.get(RELATED_FIELD);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Variable variable = (Variable) o;

    if (mandatory != variable.mandatory)
      return false;
    if (name != null ? !name.equals(variable.name) : variable.name != null)
      return false;
    if (description != null ? !description.equals(variable.description) : variable.description != null)
      return false;
    return value != null ? value.equals(variable.value) : variable.value == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (mandatory ? 1 : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  public static final class VariableBuilder {
    private String name;
    private String description;
    private boolean mandatory;
    private String value;
    private boolean fixed;
    private VariableType type = VariableType.TEXT;
    private Map<String, Object> metadata = Maps.newHashMap();

    private VariableBuilder() {}

    public static VariableBuilder aVariable() {
      return new VariableBuilder();
    }

    public VariableBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public VariableBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public VariableBuilder withMandatory(boolean mandatory) {
      this.mandatory = mandatory;
      return this;
    }

    public VariableBuilder withValue(String value) {
      this.value = value;
      return this;
    }

    public VariableBuilder withFixed(boolean fixed) {
      this.fixed = fixed;
      return this;
    }

    public VariableBuilder withEntityType(EntityType entityType) {
      if (entityType != null) {
        this.metadata.put(ENTITY_TYPE, entityType);
      }
      return this;
    }

    public VariableBuilder withType(VariableType type) {
      this.type = type;
      return this;
    }

    public VariableBuilder withArtifactType(String artifactType) {
      if (artifactType != null) {
        this.metadata.put(ARTIFACT_TYPE, artifactType);
      }
      return this;
    }

    public VariableBuilder withRelatedField(String relatedField) {
      if (relatedField != null) {
        this.metadata.put(RELATED_FIELD, relatedField);
      }
      return this;
    }

    public VariableBuilder withStateType(StateType stateType) {
      this.metadata.put(STATE_TYPE, stateType);
      return this;
    }
    public VariableBuilder withMetadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Variable build() {
      Variable variable = new Variable();
      variable.setName(name);
      variable.setDescription(description);
      variable.setMandatory(mandatory);
      variable.setValue(value);
      variable.setFixed(fixed);
      variable.setType(type);
      variable.setMetadata(metadata);
      return variable;
    }
  }
}
