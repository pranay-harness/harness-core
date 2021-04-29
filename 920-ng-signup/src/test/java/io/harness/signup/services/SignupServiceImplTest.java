package io.harness.signup.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.rule.OwnerRule.NATHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.signup.SignupTestBase;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.impl.SignupServiceImpl;
import io.harness.signup.validator.SignupValidator;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(GTM)
public class SignupServiceImplTest extends SignupTestBase {
  @Inject @InjectMocks SignupServiceImpl signupServiceImpl;
  @Mock SignupValidator signupValidator;
  @Mock AccountService accountService;
  @Mock OrganizationService organizationService;
  @Mock UserClient userClient;
  @Mock ReCaptchaVerifier reCaptchaVerifier;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = NATHAN)
  @Category(UnitTests.class)
  public void testSignup() throws IOException {
    String email = "test@test.com";
    SignupDTO signupDTO = SignupDTO.builder().email(email).password("admin12345").build();
    AccountDTO accountDTO = AccountDTO.builder().identifier("12345").build();
    UserInfo newUser = UserInfo.builder().email(email).build();

    doNothing().when(signupValidator).validateSignup(any(SignupDTO.class));
    when(accountService.createAccount(signupDTO)).thenReturn(accountDTO);

    Call<RestResponse<UserInfo>> createUserCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.createNewUser(any(UserRequestDTO.class))).thenReturn(createUserCall);

    UserInfo returnedUser = signupServiceImpl.signup(signupDTO, null);

    verify(organizationService, times(1)).create(eq(accountDTO.getIdentifier()), any(OrganizationDTO.class));
    verify(reCaptchaVerifier, times(1)).verifyInvisibleCaptcha(anyString());
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }

  @Test
  @Owner(developers = NATHAN)
  @Category(UnitTests.class)
  public void testSignupOAuth() throws IOException {
    String email = "test@test.com";
    String name = "testName";
    OAuthSignupDTO oAuthSignupDTO = OAuthSignupDTO.builder().email(email).name(name).build();
    AccountDTO accountDTO = AccountDTO.builder().identifier("12345").build();
    UserInfo newUser = UserInfo.builder().email(email).build();

    doNothing().when(signupValidator).validateEmail(eq(email));
    when(accountService.createAccount(any(SignupDTO.class))).thenReturn(accountDTO);

    Call<RestResponse<UserInfo>> createUserCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.createNewOAuthUser(any(UserRequestDTO.class))).thenReturn(createUserCall);

    UserInfo returnedUser = signupServiceImpl.oAuthSignup(oAuthSignupDTO);

    verify(organizationService, times(1)).create(eq(accountDTO.getIdentifier()), any(OrganizationDTO.class));
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }
}
