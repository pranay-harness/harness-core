package software.wings.security.authentication;

import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.joda.time.DateTimeUtils;
import software.wings.beans.User;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.UserService;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class TOTPAuthHandler implements TwoFactorAuthHandler {
  @Inject private UserService userService;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private EmailNotificationService emailNotificationService;

  @Override
  public User authenticate(User user, String... credentials) {
    try {
      String passcode = credentials[0];
      String totpSecret = user.getTotpSecretKey();
      if (isBlank(totpSecret)) {
        throw new WingsException(ErrorCode.INVALID_TWO_FACTOR_AUTHENTICATION_CONFIGURATION);
      }

      final int code = Integer.parseInt(passcode);
      final long currentTime = DateTimeUtils.currentTimeMillis();
      if (!TimeBasedOneTimePasswordUtil.validateCurrentNumber(
              totpSecret, code, 0, currentTime, TimeBasedOneTimePasswordUtil.DEFAULT_TIME_STEP_SECONDS)
          && !TimeBasedOneTimePasswordUtil.validateCurrentNumber(
                 totpSecret, code, 0, currentTime - 10000, TimeBasedOneTimePasswordUtil.DEFAULT_TIME_STEP_SECONDS)
          && !TimeBasedOneTimePasswordUtil.validateCurrentNumber(
                 totpSecret, code, 0, currentTime + 10000, TimeBasedOneTimePasswordUtil.DEFAULT_TIME_STEP_SECONDS)) {
        throw new WingsException(ErrorCode.INVALID_TOTP_TOKEN, USER);
      }
      return user;

    } catch (GeneralSecurityException | NumberFormatException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }
  }

  @Override
  public TwoFactorAuthenticationMechanism getAuthenticationMechanism() {
    return TwoFactorAuthenticationMechanism.TOTP;
  }

  @Override
  public TwoFactorAuthenticationSettings createTwoFactorAuthenticationSettings(User user) {
    String secretKey = generateTotpSecret();
    String otpUrl =
        generateOtpUrl(authenticationUtils.getDefaultAccount(user).getCompanyName(), user.getEmail(), secretKey);
    return TwoFactorAuthenticationSettings.builder()
        .mechanism(getAuthenticationMechanism())
        .totpqrurl(otpUrl)
        .twoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
        .email(user.getEmail())
        .userId(user.getUuid())
        .totpSecretKey(secretKey)
        .build();
  }

  @Override
  public User applyTwoFactorAuthenticationSettings(User user, TwoFactorAuthenticationSettings settings) {
    return userService.updateTwoFactorAuthenticationSettings(user, settings);
  }

  @Override
  public User disableTwoFactorAuthentication(User user) {
    user.setTwoFactorAuthenticationEnabled(false);
    user.setTotpSecretKey(null);
    user.setTwoFactorAuthenticationMechanism(null);
    return userService.update(user);
  }

  private String generateTotpSecret() {
    return TimeBasedOneTimePasswordUtil.generateBase32Secret();
  }

  private String generateOtpUrl(String companyName, String userEmailAddress, String secret) {
    return format(
        "otpauth://totp/%s:%s?secret=%s&issuer=Harness-Inc", companyName.replace(" ", "-"), userEmailAddress, secret);
  }

  @Override
  public boolean resetAndSendEmail(User user) {
    TwoFactorAuthenticationSettings settings = createTwoFactorAuthenticationSettings(user);
    applyTwoFactorAuthenticationSettings(user, settings);
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("name", user.getName());
    templateModel.put("totpSecret", settings.getTotpSecretKey());
    templateModel.put("totpUrl", settings.getTotpqrurl());

    List<String> toList = new ArrayList();
    toList.add(user.getEmail());
    EmailData emailData = EmailData.builder()
                              .to(toList)
                              .templateName("reset_2fa")
                              .templateModel(templateModel)
                              .accountId(authenticationUtils.getDefaultAccount(user).getUuid())
                              .build();
    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);
    emailNotificationService.send(emailData);
    return true;
  }
}
