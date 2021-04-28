package io.harness.signup.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.ng.core.user.UserInfo;
import io.harness.signup.dto.SignupDTO;

@OwnedBy(GTM)
public interface SignupService {
  UserInfo signup(SignupDTO dto, String captchaToken) throws WingsException;
}
