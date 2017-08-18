package software.wings.beans;

/**
 * Created by rishi on 12/21/16.
 */
public class Variable {
  private String name;
  private String description;
  private boolean mandatory;
  private String value;
  private boolean fixed;
  private EntityType entityType;

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

  public EntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
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
    private EntityType entityType;
    private VariableType type = VariableType.TEXT;

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
      this.entityType = entityType;
      return this;
    }

    public VariableBuilder withType(VariableType type) {
      this.type = type;
      return this;
    }

    public Variable build() {
      Variable variable = new Variable();
      variable.setName(name);
      variable.setDescription(description);
      variable.setMandatory(mandatory);
      variable.setValue(value);
      variable.setFixed(fixed);
      variable.setEntityType(entityType);
      variable.setType(type);
      return variable;
    }
  }
}
