package io.harness.signup.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.UserInfo;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.SignupService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@OwnedBy(GTM)
public class SignupServiceImpl implements SignupService {
  @Override
  public UserInfo signup(SignupDTO dto) {
    return UserInfo.builder().build();
  }
}
