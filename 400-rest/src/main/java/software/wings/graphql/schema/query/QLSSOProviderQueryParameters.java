package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;

@Value
@TargetModule(Module._380_CG_GRAPHQL)
public class QLSSOProviderQueryParameters {
  private String ssoProviderId;
}
