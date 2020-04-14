package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Builder;
import lombok.Value;
import org.apache.http.client.utils.URIBuilder;

@Value
@Builder
public class HttpConnectionExecutionCapability implements ExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.HTTP;

  private String url;

  private String host;
  private String scheme;
  private int port;
  private String path;
  private String query;

  @Override
  public String fetchCapabilityBasis() {
    if (url != null) {
      return url;
    }
    URIBuilder uriBuilder = new URIBuilder();
    if (isNotBlank(scheme)) {
      uriBuilder.setScheme(scheme);
    }
    uriBuilder.setHost(host);
    if (port != -1) {
      uriBuilder.setPort(port);
    }
    if (isNotBlank(path)) {
      uriBuilder.setPath('/' + path);
    }
    if (isNotBlank(query)) {
      uriBuilder.setCustomQuery(query);
    }
    return uriBuilder.toString();
  }
}
