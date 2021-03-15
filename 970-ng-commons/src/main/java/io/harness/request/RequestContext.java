package io.harness.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestContext {
  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;
}
