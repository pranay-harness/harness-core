package io.harness.request;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestContext {
  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;
}
