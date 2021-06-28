package io.harness.accesscontrol.resources.resourcegroups;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class ResourceGroupClientConfiguration {
  private ServiceHttpClientConfig resourceGroupServiceConfig;
  private String resourceGroupServiceSecret;
}
