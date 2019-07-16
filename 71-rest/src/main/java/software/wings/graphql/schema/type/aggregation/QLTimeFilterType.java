package software.wings.graphql.schema.type.aggregation;

public interface QLTimeFilterType extends QLFilterType {
  QLTimeFilter getTimeFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.TIME;
  }
}
