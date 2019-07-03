package software.wings.graphql.schema.type.aggregation.workflow;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLWorkflowFilterType {
  Application(QLDataType.STRING),
  Workflow(QLDataType.STRING);

  private QLDataType dataType;

  QLWorkflowFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}
