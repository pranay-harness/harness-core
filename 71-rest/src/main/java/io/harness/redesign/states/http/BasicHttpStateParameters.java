package io.harness.redesign.states.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.github.reinert.jjschema.Attributes;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class BasicHttpStateParameters implements StateParameters {
  @Attributes(required = true, title = "URL") String url;
  @Attributes(required = true, enums = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"}, title = "Method")
  String method;
  @Attributes(title = "Header") String header;
  @Attributes(title = "Body") String body;
  @Attributes(title = "Assertion") String assertion;
  int socketTimeoutMillis = 10000;
}