package software.wings.graphql.schema.type.aggregation.pipeline;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLPipelineFilterType {
  Application(QLDataType.STRING),
  Pipeline(QLDataType.STRING);

  private QLDataType dataType;

  QLPipelineFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}
