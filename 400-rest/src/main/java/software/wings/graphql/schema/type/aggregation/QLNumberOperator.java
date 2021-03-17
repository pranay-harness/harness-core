package software.wings.graphql.schema.type.aggregation;

/**
 * @author rktummala on 06/07/19
 */
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public enum QLNumberOperator {
  EQUALS,
  GREATER_THAN,
  GREATER_THAN_OR_EQUALS,
  IN,
  LESS_THAN,
  LESS_THAN_OR_EQUALS,
  NOT_EQUALS
}
