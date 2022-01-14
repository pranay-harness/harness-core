package software.wings.graphql.schema.type.aggregation;

/**
 * Interface for all tag aggregations
 * @author rktummala on 09/05/19
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface TagAggregation<E> {
  E getEntityType();
  String getTagName();
}
