package software.wings.graphql.schema.type.instance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public interface QLPhysicalHost {
  String getHostId();
  String getHostName();
  String getHostPublicDns();
}
