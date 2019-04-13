package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Variable.VariableBuilder.aVariable;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

import java.util.Map;

@Data
@JsonInclude(NON_NULL)
public class Variable {
  private String name;
  private String description;
  private boolean mandatory;
  private String value;
  private boolean fixed;

  public static final String ENTITY_TYPE = "entityType";
  public static final String ARTIFACT_TYPE = "artifactType";
  public static final String RELATED_FIELD = "relatedField";
  public static final String STATE_TYPE = "stateType";
  public static final String PARENT_FIELDS = "parentFields";

  private Map<String, Object> metadata = Maps.newHashMap();

  private VariableType type = VariableType.TEXT;

  public Variable cloneInternal() {
    return aVariable()
        .name(name)
        .value(value)
        .type(type)
        .description(description)
        .mandatory(mandatory)
        .fixed(fixed)
        .metadata(metadata)
        .build();
  }

  public EntityType obtainEntityType() {
    if (metadata == null) {
      return null;
    }
    Object entityType = metadata.get(ENTITY_TYPE);
    if (entityType == null) {
      return null;
    }
    return entityType instanceof EntityType ? (EntityType) entityType : EntityType.valueOf((String) entityType);
  }

  public Object obtainArtifactType() {
    if (metadata == null) {
      return null;
    }
    return metadata.get(ARTIFACT_TYPE);
  }

  public String obtainRelatedField() {
    if (metadata == null) {
      return "";
    }
    return metadata.get(RELATED_FIELD) != null ? (String) metadata.get(RELATED_FIELD) : "";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Variable variable = (Variable) o;

    if (mandatory != variable.mandatory) {
      return false;
    }
    if (name != null ? !name.equals(variable.name) : variable.name != null) {
      return false;
    }
    if (description != null ? !description.equals(variable.description) : variable.description != null) {
      return false;
    }
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

    public VariableBuilder name(String name) {
      this.name = name;
      return this;
    }

    public VariableBuilder description(String description) {
      this.description = description;
      return this;
    }

    public VariableBuilder mandatory(boolean mandatory) {
      this.mandatory = mandatory;
      return this;
    }

    public VariableBuilder value(String value) {
      this.value = value;
      return this;
    }

    public VariableBuilder fixed(boolean fixed) {
      this.fixed = fixed;
      return this;
    }

    public VariableBuilder entityType(EntityType entityType) {
      if (entityType != null) {
        this.metadata.put(ENTITY_TYPE, entityType);
      }
      return this;
    }

    public VariableBuilder type(VariableType type) {
      this.type = type;
      return this;
    }

    public VariableBuilder artifactType(String artifactType) {
      if (artifactType != null) {
        this.metadata.put(ARTIFACT_TYPE, artifactType);
      }
      return this;
    }

    public VariableBuilder relatedField(String relatedField) {
      if (relatedField != null) {
        this.metadata.put(RELATED_FIELD, relatedField);
      }
      return this;
    }

    public VariableBuilder stateType(String stateType) {
      this.metadata.put(STATE_TYPE, stateType);
      return this;
    }

    public VariableBuilder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public VariableBuilder parentFields(Map<String, String> parentFields) {
      if (!isEmpty(parentFields)) {
        this.metadata.put(PARENT_FIELDS, parentFields);
      }
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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYamlWithType {
    private String name;
    private String description;
    private boolean mandatory;
    private String value;
    private boolean fixed;

    @Builder
    public Yaml(String type, String name, String description, boolean mandatory, String value, boolean fixed) {
      super(type);
      this.name = name;
      this.description = description;
      this.mandatory = mandatory;
      this.value = value;
      this.fixed = fixed;
    }
  }
}
