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
  private boolean expressionAllowed; // Can this template expression can contain other expression

  private EntityType entityType;

  private Map<String, Object> metadata = Maps.newHashMap();

  public String getFieldName() {
    return fieldName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fieldName", fieldName)
        .add("expression", expression)
        .add("entityType", entityType)
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
    return expressionAllowed == that.expressionAllowed && Objects.equals(fieldName, that.fieldName)
        && Objects.equals(expression, that.expression) && entityType == that.entityType
        && Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, expression, expressionAllowed, entityType, metadata);
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

  public EntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }
}
