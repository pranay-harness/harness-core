package software.wings.graphql.schema.type.aggregation;

public interface QLStringFilterType extends QLFilterType {
  QLStringFilter getStringFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.STRING;
  }
}
