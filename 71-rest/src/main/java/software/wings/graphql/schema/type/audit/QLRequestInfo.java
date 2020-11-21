package software.wings.graphql.schema.type.audit;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLRequestInfo implements QLObject {
  private String url;
  private String resourcePath;
  private String requestMethod;
  private Number responseStatusCode;
  private String remoteIpAddress;
}
