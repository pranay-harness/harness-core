package io.harness.signup.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import static org.mindrot.jbcrypt.BCrypt.hashpw;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.exception.UserAlreadyPresentException;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.remote.client.RestClientUtils;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.SignupService;
import io.harness.signup.validator.SignupValidator;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
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

  private void createOrganization(String accountIdentifier) {
    OrganizationDTO dto =
        OrganizationDTO.builder().name("Default").identifier("default").description("Default Organization").build();
    organizationService.create(accountIdentifier, dto);
  }

  private UserInfo createUser(SignupDTO signupDTO, AccountDTO account) throws UserAlreadyPresentException {
    String passwordHash = hashpw(signupDTO.getPassword(), BCrypt.gensalt());
    List<AccountDTO> accountList = new ArrayList();
    accountList.add(account);

    String name = account.getName();

    UserRequestDTO userRequest = UserRequestDTO.builder()
                                     .email(signupDTO.getEmail())
                                     .name(name)
                                     .passwordHash(passwordHash)
                                     .accountName(account.getName())
                                     .companyName(account.getCompanyName())
                                     .accounts(accountList)
                                     .emailVerified(false)
                                     .defaultAccountId(account.getIdentifier())
                                     .build();

    return RestClientUtils.getResponse(userClient.createNewUser(userRequest));
  }
}
