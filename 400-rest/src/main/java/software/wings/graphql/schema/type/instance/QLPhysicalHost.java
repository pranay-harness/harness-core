package software.wings.graphql.schema.type.instance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLPhysicalHost {
  String getHostId();
  String getHostName();
  String getHostPublicDns();
}
