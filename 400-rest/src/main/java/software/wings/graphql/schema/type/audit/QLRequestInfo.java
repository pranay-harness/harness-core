package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLRequestInfo implements QLObject {
  private String url;
  private String resourcePath;
  private String requestMethod;
  private Number responseStatusCode;
  private String remoteIpAddress;
}
