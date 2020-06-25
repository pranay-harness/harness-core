package io.harness.cvng.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

@Value
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
