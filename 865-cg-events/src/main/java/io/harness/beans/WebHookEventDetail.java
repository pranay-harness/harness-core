package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.http.HttpHeaderConfig;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebHookEventDetail extends EventDetail {
  private int responseStatusCode;
  private List<HttpHeaderConfig> requestHeaders;
  private String url;
  private List<HttpHeaderConfig> responseHeaders;
  private Object responseBody;
}
