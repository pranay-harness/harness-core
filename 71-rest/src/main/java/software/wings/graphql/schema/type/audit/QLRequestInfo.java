package software.wings.graphql.schema.type.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
public class QLRequestInfo implements QLObject {
  private String url;
  private String resourcePath;
  private String requestMethod;
  private Number responseStatusCode;
  private String remoteIpAddress;
}
