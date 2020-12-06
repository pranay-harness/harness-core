package software.wings.graphql.schema.type.aggregation;

/**
 * Interface for all tag aggregations
 * @author rktummala on 09/05/19
 */
public interface TagAggregation<E> {
  E getEntityType();
  String getTagName();
}
