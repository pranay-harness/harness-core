package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

/**
 * Created by sgurubelli on 8/11/17.
 */
public class TemplateExpression {
  private String fieldName;
  private String expression;
  private boolean expressionAllowed = true; // Can this template expression can contain other expression
  private String description;
  private boolean mandatory;
  private Map<String, Object> metadata = Maps.newHashMap();

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public boolean isExpressionAllowed() {
    return expressionAllowed;
  }

  public void setExpressionAllowed(boolean expressionAllowed) {
    this.expressionAllowed = expressionAllowed;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fieldName", fieldName)
        .add("expression", expression)
        .add("metadata", metadata)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    TemplateExpression that = (TemplateExpression) o;
    return Objects.equals(fieldName, that.fieldName) && Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, expression);
  }

  public static final class Builder {
    private String fieldName;
    private String expression;
    private boolean expressionAllowed = true;
    private String description;
    private boolean mandatory;
    private EntityType entityType;
    private Map<String, Object> metadata = Maps.newHashMap();

    private Builder() {}

    public static Builder aTemplateExpression() {
      return new Builder();
    }

    public Builder withFieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public Builder withExpression(String expression) {
      this.expression = expression;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withExpressionAllowed(boolean expressionAllowed) {
      this.expressionAllowed = expressionAllowed;
      return this;
    }

    public Builder withMandatory(boolean mandatory) {
      this.mandatory = mandatory;
      return this;
    }

    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withMetadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public TemplateExpression build() {
      TemplateExpression templateExpression = new TemplateExpression();
      templateExpression.setFieldName(fieldName);
      templateExpression.setExpression(expression);
      templateExpression.setExpressionAllowed(expressionAllowed);
      templateExpression.setMandatory(mandatory);
      templateExpression.setDescription(description);
      templateExpression.setMetadata(metadata);

      return templateExpression;
    }
  }
}
