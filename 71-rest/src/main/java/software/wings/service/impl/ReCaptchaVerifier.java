package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import software.wings.resources.error.Error;
import software.wings.security.authentication.recaptcha.ReCaptchaClient;
import software.wings.security.authentication.recaptcha.ReCaptchaClientBuilder;
import software.wings.security.authentication.recaptcha.VerificationStatus;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Singleton
public class ReCaptchaVerifier {
  private ReCaptchaClientBuilder reCaptchaClientBuilder;

  @Inject
  public ReCaptchaVerifier(ReCaptchaClientBuilder reCaptchaClientBuilder) {
    this.reCaptchaClientBuilder = reCaptchaClientBuilder;
  }

  public void verify(String captchaResponseToken) {
    Optional<Error> err;

    try {
      err = verifyCaptcha(captchaResponseToken);
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
  private Optional<Error> verifyCaptcha(String captchaToken) {
    String secret = System.getenv("RECAPTCHA_SECRET");

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
