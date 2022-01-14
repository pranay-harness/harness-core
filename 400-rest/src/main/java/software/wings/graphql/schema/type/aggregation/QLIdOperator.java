package software.wings.graphql.schema.type.aggregation;

/**
 * @author rktummala on 06/07/19
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL) public enum QLIdOperator { EQUALS, IN, NOT_NULL, NOT_IN, LIKE }
