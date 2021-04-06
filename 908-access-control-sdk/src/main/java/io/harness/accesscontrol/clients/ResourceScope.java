package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class ResourceScope {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;

  public static ResourceScope of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ResourceScope.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
