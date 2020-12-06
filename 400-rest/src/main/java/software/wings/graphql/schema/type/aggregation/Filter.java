package software.wings.graphql.schema.type.aggregation;

public interface Filter<O, V> {
  O getOperator();
  V[] getValues();
}
