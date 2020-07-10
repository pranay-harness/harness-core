package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitHTTPAuthenticationDTO implements GitAuthenticationDTO {
  @JsonProperty("type") GitConnectionType gitType;
  String url;
  String username;
  String passwordReference;
  String branchName;
}
