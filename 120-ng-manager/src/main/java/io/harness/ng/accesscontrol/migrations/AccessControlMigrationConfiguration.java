package io.harness.ng.accesscontrol.migrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.remote.client.ServiceHttpClientConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessControlMigrationConfiguration {
  private boolean enabled;
  @JsonProperty("roleAssignmentClientConfig") private ServiceHttpClientConfig roleAssignmentClientConfig;
  @JsonProperty("roleAssignmentClientSecret") private String roleAssignmentClientSecret;
}
