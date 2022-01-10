/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.authenticationservice.recaptcha;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Error;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class ReCaptchaVerifier {
  private ReCaptchaClientBuilder reCaptchaClientBuilder;

  @Inject
  public ReCaptchaVerifier(ReCaptchaClientBuilder reCaptchaClientBuilder) {
    this.reCaptchaClientBuilder = reCaptchaClientBuilder;
  }

  public void verify(String captchaResponseToken) {
    Optional<Error> err;

    try {
      err = verifyCaptcha(captchaResponseToken, false);
    } catch (Exception e) {
      // catching exception because login should not be blocked because of error in verifying captcha
      log.error("Exception occurred while trying to verify captcha.", e);
      return;
    }

    if (err.isPresent()) {
      throw new WingsException(err.get().getCode(), err.get().getMessage());
    }
  }

  public void verifyInvisibleCaptcha(String captchaResponseToken) {
    Optional<Error> err;

    try {
      err = verifyCaptcha(captchaResponseToken, true);
    } catch (Exception e) {
      // catching exception because login should not be blocked because of error in verifying captcha
      log.error("Exception occurred while trying to verify captcha.", e);
      return;
    }

    if (err.isPresent()) {
      throw new WingsException(err.get().getCode(), err.get().getMessage());
    }
  }

  /**
   * @param captchaToken - captcha token sent by FE
   * @return optional error, presence of which should be treated as failed captcha verification
   */
  private Optional<Error> verifyCaptcha(String captchaToken, boolean invisibleCaptcha) {
    String captchaEnvSecretName = invisibleCaptcha ? "INVISIBLE_RECAPTCHA_SECRET" : "RECAPTCHA_SECRET";
    String secret = System.getenv(captchaEnvSecretName);
    if (StringUtils.isEmpty(secret)) {
      log.error(
          "Could not find captcha secret. Marking captcha verification as pass since it is an error on our side.");
      return Optional.empty();
    }

    try {
      ReCaptchaClient reCaptchaClient = reCaptchaClientBuilder.getInstance();
      Response<VerificationStatus> verificationStatusResponse =
          reCaptchaClient.siteverify(secret, captchaToken).execute();

      if (!verificationStatusResponse.isSuccessful()) {
        return Optional.of(new Error(ErrorCode.GENERAL_ERROR, "Error verifying captcha"));
      }

      if (!verificationStatusResponse.body().getSuccess()) {
        log.error("Captcha verification failed. Response: {}", verificationStatusResponse.body());
        return Optional.of(new Error(ErrorCode.INVALID_CAPTCHA_TOKEN, "Invalid Captcha Token"));
      }

    } catch (IOException e) {
      return Optional.of(new Error(ErrorCode.GENERAL_ERROR, "Could not verify captcha."));
    }

    return Optional.empty();
  }
}
