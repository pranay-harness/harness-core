package software.wings.graphql.datafetcher;

/**
 * The idea of this enum to keep track of fields
 * that are exposed in a schema and to also
 * write test to make sure we are duplicating
 * the usage of fields in any way.
 */
public enum SchemaFieldsEnum {
  WORKFLOW("workflow"),
  WORKFLOW_LIST("workflows"),
  WORKFLOW_EXECUTION_STATUS("workFlowExecution");

  private String fieldName;

  SchemaFieldsEnum(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return this.fieldName;
  }
}
