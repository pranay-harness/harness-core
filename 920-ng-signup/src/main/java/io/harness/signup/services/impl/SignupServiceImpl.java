package io.harness.signup.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.UserInfo;
import io.harness.remote.client.RestClientUtils;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.SignupService;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(GTM)
public class SignupServiceImpl implements SignupService {
  private final UserClient userClient;
  @Override
  public UserInfo signup(SignupDTO dto) {
    Optional<UserInfo> userInfo = RestClientUtils.getResponse(userClient.getUserByEmailId(dto.getEmail()));

    return userInfo.get();
  }
}
