package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class UserGroupClientConfiguration {
  private ServiceHttpClientConfig userGroupServiceConfig;
  private String userGroupServiceSecret;
}
