package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLEnum {
  String getStringValue();
}
