package software.wings.security.authentication;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.exception.WingsException.USER;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.UserService;

import java.security.GeneralSecurityException;
import java.util.Collections;

@Singleton
public class TOTPAuthHandler implements TwoFactorAuthHandler {
  @Inject private UserService userService;
  @Inject private AuthenticationUtil authenticationUtil;
  @Inject private EmailNotificationService emailNotificationService;

  @Override
  public User authenticate(User user, String... credentials) {
    try {
      String passcode = credentials[0];
      String totpSecret = user.getTotpSecretKey();
      if (isBlank(totpSecret)) {
        throw new WingsException(ErrorCode.INVALID_TWO_FACTOR_AUTHENTICATION_CONFIGURATION);
      }
      String currentSecret = TimeBasedOneTimePasswordUtil.generateCurrentNumberString(totpSecret);

      if (!currentSecret.equals(passcode)) {
        throw new WingsException(ErrorCode.INVALID_TOTP_TOKEN, USER);
      }
      return user;

    } catch (GeneralSecurityException e) {
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
        generateOtpUrl(authenticationUtil.getPrimaryAccount(user).get().getCompanyName(), user.getEmail(), secretKey);
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
    user.setTwoFactorAuthenticationEnabled(settings.isTwoFactorAuthenticationEnabled());
    user.setTwoFactorAuthenticationMechanism(settings.getMechanism());
    user.setTotpSecretKey(settings.getTotpSecretKey());
    return userService.update(user);
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
    StringBuilder sb = new StringBuilder(100);
    return sb.append("otpauth://totp/")
        .append(companyName.replace(" ", "-"))
        .append(":")
        .append(userEmailAddress)
        .append("?secret=")
        .append(secret)
        .append("&issuer=Harness-Inc")
        .toString();
  }

  @Override
  public boolean resetAndSendEmail(User user) {
    TwoFactorAuthenticationSettings settings = createTwoFactorAuthenticationSettings(user);
    applyTwoFactorAuthenticationSettings(user, settings);
    EmailData emailData = EmailData.builder()
                              .to(asList(user.getEmail()))
                              .templateName("reset_2fa")
                              .templateModel(ImmutableMap.of("name", user.getName(), "totpSecret",
                                  settings.getTotpSecretKey(), "totpUrl", settings.getTotpqrurl()))
                              .system(true)
                              .build();
    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);
    emailNotificationService.send(emailData);
    return true;
  }
}
