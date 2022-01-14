package software.wings.graphql.schema.type.aggregation;

public interface QLNumberFilterType extends QLFilterType {
  QLNumberFilter getNumberFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.NUMBER;
  }
}
