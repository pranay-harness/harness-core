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
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.yaml.BaseYamlWithType;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(NON_NULL)
public class Variable {
  private String name;
  private String description;
  private boolean mandatory;
  private String value;
  private boolean fixed;
  private String allowedValues;
  private List<String> allowedList;
  private transient List<ArtifactStreamSummary> artifactStreamSummaries;

  public static final String ENTITY_TYPE = "entityType";
  public static final String ARTIFACT_TYPE = "artifactType";
  public static final String RELATED_FIELD = "relatedField";
  public static final String STATE_TYPE = "stateType";
  public static final String PARENT_FIELDS = "parentFields";

  private Map<String, Object> metadata = Maps.newHashMap();

  private VariableType type = VariableType.TEXT;

  public Variable() {}

  public Variable(String name, String description, boolean mandatory, String value, boolean fixed, String allowedValues,
      List<String> allowedList, Map<String, Object> metadata, VariableType type) {
    this.name = name;
    this.description = description;
    this.mandatory = mandatory;
    this.value = value;
    this.fixed = fixed;
    this.allowedValues = allowedValues;
    this.allowedList = allowedList;
    this.metadata = metadata;
    this.type = type;
  }

  public Variable cloneInternal() {
    return aVariable()
        .name(name)
        .value(value)
        .type(type)
        .description(description)
        .mandatory(mandatory)
        .fixed(fixed)
        .metadata(metadata)
        .allowedValues(allowedValues)
        .allowedList(allowedList)
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
    private String allowedValues;
    private List<String> allowedList;

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

    public VariableBuilder allowedValues(String allowedValues) {
      this.allowedValues = allowedValues;
      return this;
    }

    public VariableBuilder allowedList(List<String> allowedList) {
      this.allowedList = allowedList;
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
      variable.setAllowedValues(allowedValues);
      variable.setAllowedList(allowedList);
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
    private String allowedValues; // todo: toYaml() don't convert to allowedValues
    private List<AllowedValueYaml> allowedList;

    @Builder
    public Yaml(String type, String name, String description, boolean mandatory, String value, boolean fixed,
        String allowedValues, List<AllowedValueYaml> allowedList) {
      super(type);
      this.name = name;
      this.description = description;
      this.mandatory = mandatory;
      this.value = value;
      this.fixed = fixed;
      this.allowedValues = allowedValues;
      this.allowedList = allowedList;
    }
  }
}
