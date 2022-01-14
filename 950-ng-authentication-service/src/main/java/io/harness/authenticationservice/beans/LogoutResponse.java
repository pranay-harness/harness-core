package io.harness.authenticationservice.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@OwnedBy(PL)
@Data
public class LogoutResponse {
  private String logoutUrl;
}
