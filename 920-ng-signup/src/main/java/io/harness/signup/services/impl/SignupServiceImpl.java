package io.harness.signup.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import static org.mindrot.jbcrypt.BCrypt.hashpw;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.remote.client.RestClientUtils;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.SignupService;
import io.harness.signup.validator.SignupValidator;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(GTM)
public class SignupServiceImpl implements SignupService {
  private AccountService accountService;
  private UserClient userClient;
  private SignupValidator signupValidator;
  private OrganizationService organizationService;
  private ReCaptchaVerifier reCaptchaVerifier;

  @Override
  public UserInfo signup(SignupDTO dto, String captchaToken) throws WingsException {
    reCaptchaVerifier.verifyInvisibleCaptcha(captchaToken);

    signupValidator.validateSignup(dto);

    AccountDTO account = accountService.createAccount(dto);

    createOrganization(account.getIdentifier());

    return createUser(dto, account);
  }

  @Override
  public UserInfo oAuthSignup(OAuthSignupDTO dto) {
    signupValidator.validateEmail(dto.getEmail());

    SignupDTO signupDTO = SignupDTO.builder().email(dto.getEmail()).utmInfo(dto.getUtmInfo()).build();

    AccountDTO account = accountService.createAccount(signupDTO);

    createOrganization(account.getIdentifier());

    return createOAuthUser(dto, account);
  }

  private void createOrganization(String accountIdentifier) {
    OrganizationDTO dto =
        OrganizationDTO.builder().name("Default").identifier("default").description("Default Organization").build();
    organizationService.create(accountIdentifier, dto);
  }

  private UserInfo createUser(SignupDTO signupDTO, AccountDTO account) {
    String passwordHash = hashpw(signupDTO.getPassword(), BCrypt.gensalt());

    String name = account.getName();

    UserRequestDTO userRequest = UserRequestDTO.builder()
                                     .email(signupDTO.getEmail())
                                     .name(name)
                                     .passwordHash(passwordHash)
                                     .accountName(account.getName())
                                     .companyName(account.getCompanyName())
                                     .accounts(Arrays.asList(account))
                                     .emailVerified(false)
                                     .defaultAccountId(account.getIdentifier())
                                     .build();

    return RestClientUtils.getResponse(userClient.createNewUser(userRequest));
  }

  private UserInfo createOAuthUser(OAuthSignupDTO oAuthSignupDTO, AccountDTO account) {
    UserRequestDTO userRequest = UserRequestDTO.builder()
                                     .email(oAuthSignupDTO.getEmail())
                                     .name(oAuthSignupDTO.getName())
                                     .accountName(account.getName())
                                     .companyName(account.getCompanyName())
                                     .accounts(Arrays.asList(account))
                                     .emailVerified(true)
                                     .defaultAccountId(account.getIdentifier())
                                     .build();

    return RestClientUtils.getResponse(userClient.createNewOAuthUser(userRequest));
  }
}
