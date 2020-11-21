package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SplunkConnector implements Connector {
  String accountId;
  String baseUrl;
  String username;
  String password;

  @Override
  @JsonIgnore
  public Map<String, String> collectionHeaders() {
    return Collections.emptyMap();
  }

  @Override
  @JsonIgnore
  public Map<String, String> collectionParams() {
    return Collections.emptyMap();
  }
}
