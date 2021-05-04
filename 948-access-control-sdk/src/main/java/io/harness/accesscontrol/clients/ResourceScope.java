package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@JsonInclude(Include.NON_NULL)
public class ResourceScope {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;

  public static ResourceScope of(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ResourceScope.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
