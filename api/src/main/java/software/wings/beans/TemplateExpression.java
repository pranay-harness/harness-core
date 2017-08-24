package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Created by sgurubelli on 8/11/17.
 */
public class TemplateExpression {
  private String fieldName;
  private String expression;
  private boolean expressionAllowed = true; // Can this template expression can contain other expression

  private boolean mandatory;

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

  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }
}
