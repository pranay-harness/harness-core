package software.wings.integration;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.rule.OwnerRule.MARK;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static software.wings.beans.User.Builder.anUser;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.UserInvite.UserInviteKeys;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.resources.UserResource.ResendInvitationEmailRequest;
import software.wings.security.AuthenticationFilter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;

/**
 * Created by rsingh on 4/24/17.
 */
@Slf4j
public class UserServiceIntegrationTest extends BaseIntegrationTest {
  private final String validEmail = "raghu" + System.currentTimeMillis() + "@wings.software";
  @Inject private SecretManager secretManager;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDisableEnableUser() {
    User user = userService.getUserByEmail(defaultEmail);
    String userId = user.getUuid();
    String userAccountId = user.getDefaultAccountId();

    // 1. Disable user
    WebTarget target = client.target(API_BASE + "/users/disable/" + userId + "?accountId=" + userAccountId);
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).put(
        entity(defaultEmail, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource()).isTrue();

    // 2. User should be disabled after disable operation above
    user = userService.getUserByEmail(defaultEmail);
    assertThat(user.isDisabled()).isTrue();

    // 3. Once the user is disabled, its logintype call will fail with 401 unauthorized error.
    target = client.target(API_BASE + "/users/logintype?userName=" + defaultEmail + "&accountId=" + userAccountId);
    try {
      getRequestBuilder(target).get(new GenericType<RestResponse<LoginTypeResponse>>() {});
      fail("NotAuthorizedException is expected");
    } catch (NotAuthorizedException e) {
      // Expected 'HTTP 401 Unauthorized' exception here.
    }

    // 4. Re-enable user
    target = client.target(API_BASE + "/users/enable/" + userId + "?accountId=" + userAccountId);
    restResponse = getRequestBuilderWithAuthHeader(target).put(
        entity(defaultEmail, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource()).isTrue();

    // 5. User should not be disabled after enabled above
    user = userService.getUserByEmail(defaultEmail);
    assertThat(user.isDisabled()).isFalse();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testBlankEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertThat(restResponse.getResource()).isFalse();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testInvalidEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertThat(restResponse.getResource()).isFalse();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDomainNotAllowed() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz@some-domain.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, responseMessage.getCode());
    assertThat(restResponse.getResource()).isFalse();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUserExists() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=admin@harness.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_ALREADY_REGISTERED, responseMessage.getCode());
    assertThat(restResponse.getResource()).isFalse();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=" + validEmail);
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource()).isTrue();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidEmailWithSpace() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=%20" + validEmail + "%20");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource()).isTrue();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSwitchAccount() {
    WebTarget target = client.target(API_BASE + "/users/switch-account?accountId=" + accountId);
    RestResponse<User> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<User>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    User user = restResponse.getResource();
    assertNotNull(user);
    String jwtToken = user.getToken();
    assertNotNull(jwtToken);

    // Switch again, a new token should be generated.
    restResponse = getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<User>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    user = restResponse.getResource();
    assertNotNull(user);
    String newJwtToken = user.getToken();
    assertNotEquals(jwtToken, newJwtToken);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetLoginTypeWithAccountId() {
    WebTarget target =
        client.target(API_BASE + "/users/logintype?userName=" + adminUserEmail + "&accountId=" + accountId);
    RestResponse<LoginTypeResponse> restResponse =
        target.request().get(new GenericType<RestResponse<LoginTypeResponse>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    LoginTypeResponse loginTypeResponse = restResponse.getResource();
    assertNotNull(loginTypeResponse);
    assertEquals(AuthenticationMechanism.USER_PASSWORD, loginTypeResponse.getAuthenticationMechanism());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDefaultLoginWithAccountId() {
    WebTarget target = client.target(API_BASE + "/users/login?accountId=" + defaultAccountId);
    String basicAuthValue =
        "Basic " + encodeBase64String(format("%s:%s", adminUserEmail, new String(adminPassword)).getBytes());
    RestResponse<User> restResponse =
        target.request().header("Authorization", basicAuthValue).get(new GenericType<RestResponse<User>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    User user = restResponse.getResource();
    assertNotNull(user);
    assertNotNull(user.getToken());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSetDefaultAccount() {
    WebTarget target = client.target(API_BASE + "/users/set-default-account/" + accountId);
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).put(
        entity(accountId, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResponseMessages()).isEmpty();
    Boolean result = restResponse.getResource();
    assertNotNull(result);
    assertThat(result).isTrue();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLoginUserUsingIdentityServiceAuth() throws IllegalAccessException {
    WebTarget target = client.target(API_BASE + "/users/user/login?email=" + adminUserEmail);

    String identityServiceToken = generateIdentityServiceJwtToken();
    RestResponse<User> response =
        target.request()
            .header(HttpHeaders.AUTHORIZATION, AuthenticationFilter.IDENTITY_SERVICE_PREFIX + identityServiceToken)
            .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    User user = response.getResource();
    assertNotNull(user);
    assertNotNull(user.getToken());
    assertNotNull(user.getAccounts());
    assertThat(user.getAccounts().size() > 0).isTrue();
    assertThat(user.getSupportAccounts().size() > 0).isTrue();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetUserUsingIdentityServiceAuth() throws IllegalAccessException {
    WebTarget target = client.target(API_BASE + "/users/user");

    User adminUser = userService.getUserByEmail(adminUserEmail);
    String identityServiceToken = generateIdentityServiceJwtToken();
    RestResponse<User> response =
        target.request()
            .header(HttpHeaders.AUTHORIZATION, AuthenticationFilter.IDENTITY_SERVICE_PREFIX + identityServiceToken)
            .header(AuthenticationFilter.USER_IDENTITY_HEADER, adminUser.getUuid())
            .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    User user = response.getResource();
    assertNotNull(user);
    assertNotNull(user.getAccounts());
    assertThat(user.getAccounts().size() > 0).isTrue();
    assertThat(user.getSupportAccounts().size() > 0).isTrue();
  }

  private String generateIdentityServiceJwtToken() throws IllegalAccessException {
    MainConfiguration configuration = new MainConfiguration();
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setJwtIdentityServiceSecret("HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhwwn0K4Ttr0uoOxSsEVYNrUU=");
    configuration.setPortal(portalConfig);
    FieldUtils.writeField(secretManager, "configuration", configuration, true);
    return secretManager.generateJWTToken(new HashMap<>(), JWT_CATEGORY.IDENTITY_SERVICE_SECRET);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testTrialSignupSuccess() {
    final String name = "Mark Lu";
    final String pwd = "somepwd123456";
    final String email = "mark" + System.currentTimeMillis() + "@harness.io";

    UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                .withAccountId(accountId)
                                .withName(name)
                                .withCompanyName("Freemium Inc")
                                .withAccountName("Freemium Inc")
                                .withEmail(email)
                                .build();
    userInvite.setPassword(pwd.toCharArray());

    WebTarget target = client.target(API_BASE + "/users/new-trial");
    // Trial signup with just one email address, nothing else.
    RestResponse<Boolean> response =
        target.request().post(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    assertThat(response.getResource()).isTrue();

    // Trial signup again using the same email should succeed. A new verification email will be sent.
    response = target.request().post(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    assertThat(response.getResource()).isTrue();

    userInvite = wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get();
    assertNotNull(userInvite);
    assertThat(userInvite.isCompleted()).isFalse();
    assertEquals(name, userInvite.getName());
    String inviteId = userInvite.getUuid();
    assertNotNull(inviteId);

    // Complete the trial invitation.
    User user = completeTrialUserInviteAndSignin(inviteId);
    assertEquals(name, user.getName());
    assertNotNull(user.getToken());

    // Verify the account get created.
    Account account = accountService.get(accountId);
    assertNotNull(account);

    // Verify the trial user is created, assigned to proper account and with the account admin roles.
    User savedUser = wingsPersistence.createQuery(User.class).filter(UserKeys.email, email).get();
    assertNotNull(savedUser);
    assertThat(savedUser.isEmailVerified()).isTrue();
    assertEquals(name, savedUser.getName());
    assertEquals(1, savedUser.getAccounts().size());

    userInvite.setPassword(pwd.toCharArray());
    // Trial signup a few more time using the same email will trigger the rejection, and the singup result will be
    // false.
    for (int i = 0; i < UserServiceImpl.REGISTRATION_SPAM_THRESHOLD; i++) {
      response =
          target.request().post(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    }
    assertThat(response.getResponseMessages()).isEmpty();
    assertThat(response.getResource()).isFalse();

    // Delete the user just created as a cleanup
    userService.delete(savedUser.getAccounts().get(0).getUuid(), savedUser.getUuid());

    // Verify user is deleted
    assertThat(userService.getUserByEmail(email)).isNull();
    // Verify user invite is deleted
    assertThat(wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get()).isNull();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupSuccess() {
    final String name = "Raghu Singh";
    final String email = "abc" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = "some account" + System.currentTimeMillis();
    final String companyName = "some company" + System.currentTimeMillis();
    WebTarget target = client.target(API_BASE + "/users");
    RestResponse<User> response = target.request().post(entity(anUser()
                                                                   .withName(name)
                                                                   .withEmail(email)
                                                                   .withPassword(pwd)
                                                                   .withRoles(getAccountAdminRoles())
                                                                   .withAccountName(accountName)
                                                                   .withCompanyName(companyName)
                                                                   .build(),
                                                            APPLICATION_JSON),
        new GenericType<RestResponse<User>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    final User savedUser = response.getResource();
    assertEquals(name, savedUser.getName());
    assertEquals(email, savedUser.getEmail());
    assertThat(savedUser.getPassword()).isNull();
    assertEquals(1, savedUser.getAccounts().size());
    assertEquals(accountName, savedUser.getAccounts().get(0).getAccountName());
    assertEquals(companyName, savedUser.getAccounts().get(0).getCompanyName());

    // Delete the user just created as a cleanup
    userService.delete(savedUser.getAccounts().get(0).getUuid(), savedUser.getUuid());

    // Verify user is deleted
    assertThat(userService.getUserByEmail(email)).isNull();
    // Verify user invite is deleted
    assertThat(wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get()).isNull();
  }

  @Test
  @Owner(emails = MARK, intermittent = true)
  @Category(IntegrationTests.class)
  public void testUserInviteSignupAndSignInSuccess() {
    final String name = "Mark Lu";
    final String email = "abc" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    UserInvite userInvite = inviteUser(accountId, name, email);
    assertNotNull(userInvite.getUuid());
    assertThat(userInvite.isCompleted()).isFalse();
    assertEquals(email, userInvite.getEmail());

    userInvite.setName(name);
    userInvite.setPassword(pwd);
    userInvite.setAgreement(true);
    completeUserInviteAndSignIn(accountId, userInvite.getUuid(), userInvite);

    UserInvite retrievedUserInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    assertThat(retrievedUserInvite.isCompleted()).isTrue();

    // Delete the user just created as a cleanup
    User user = userService.getUserByEmail(email);
    userService.delete(accountId, user.getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testInvalidUserInviteShouldFail() {
    String invalidInviteId = UUIDGenerator.generateUuid();
    UserInvite userInvite = new UserInvite();
    userInvite.setAccountId(accountId);
    userInvite.setUuid(invalidInviteId);
    userInvite.setName("Test Invitation");
    WebTarget target = client.target(API_BASE + "/users/invites/" + invalidInviteId + "?accountId=" + accountId);
    try {
      target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<User>>() {});
      fail("HTTP 401 not authorized exception is expected.");
    } catch (NotAuthorizedException e) {
      // HTTP 400 Bad Request exception is expected.
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupSuccessWithSpaces() throws IOException {
    final String name = "  Brad  Pitt    ";
    final String email = "xyz" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   " + System.currentTimeMillis();
    final String companyName = "  star   wars    enterprise   " + System.currentTimeMillis();
    WebTarget target = client.target(API_BASE + "/users");

    RestResponse<User> response = null;
    try {
      response = target.request().post(entity(anUser()
                                                  .withName(name)
                                                  .withEmail(email)
                                                  .withPassword(pwd)
                                                  .withRoles(getAccountAdminRoles())
                                                  .withAccountName(accountName)
                                                  .withCompanyName(companyName)
                                                  .build(),
                                           APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
    } catch (BadRequestException e) {
      logger.info(new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity())));
      Assert.fail();
    }
    assertThat(response.getResponseMessages()).isEmpty();
    final User savedUser = response.getResource();
    assertEquals(name.trim(), savedUser.getName());
    assertEquals(email.trim(), savedUser.getEmail());
    assertThat(savedUser.getPassword()).isNull();
    assertEquals(1, savedUser.getAccounts().size());
    assertEquals(accountName.trim(), savedUser.getAccounts().get(0).getAccountName());
    assertEquals(companyName.trim(), savedUser.getAccounts().get(0).getCompanyName());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupEmailWithSpace() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "  xyz@wings  ";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.INVALID_ARGUMENT, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupBadEmail() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@wings";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.INVALID_EMAIL, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupEmailExists() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "admin@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with existing email");
    } catch (ClientErrorException e) {
      assertEquals(HttpStatus.SC_CONFLICT, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.USER_ALREADY_REGISTERED, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testAccountCreationWithKms() {
    loginAdminUser();
    User user = wingsPersistence.createQuery(User.class).filter(UserKeys.email, "admin@harness.io").get();

    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(user.getUuid()))
                                            .actions(Sets.newHashSet(Action.READ))
                                            .build();
    wingsPersistence.save(harnessUserGroup);

    Account account = Account.Builder.anAccount()
                          .withAccountName(UUID.randomUUID().toString())
                          .withCompanyName(UUID.randomUUID().toString())
                          .withLicenseInfo(LicenseInfo.builder()
                                               .accountType(AccountType.PAID)
                                               .accountStatus(AccountStatus.ACTIVE)
                                               .licenseUnits(InstanceLimitProvider.defaults(AccountType.PAID))
                                               .build())

                          .build();

    assertThat(accountService.exists(account.getAccountName())).isFalse();
    assertThat(accountService.getByName(account.getCompanyName())).isNull();

    WebTarget target = client.target(API_BASE + "/users/account");
    RestResponse<Account> response = getRequestBuilderWithAuthHeader(target).post(
        entity(account, APPLICATION_JSON), new GenericType<RestResponse<Account>>() {});

    assertNotNull(response.getResource());
    assertThat(accountService.exists(account.getAccountName())).isTrue();
    assertNotNull(accountService.getByName(account.getCompanyName()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testResendInvitationAndCompleteInvitation() {
    Account adminAccount =
        wingsPersistence.createQuery(Account.class).filter(AccountKeys.accountName, defaultAccountName).get();
    String accountId = adminAccount.getUuid();

    ResendInvitationEmailRequest request = new ResendInvitationEmailRequest();
    request.setEmail(adminUserEmail);

    WebTarget target = client.target(API_BASE + "/users/resend-invitation-email?accountId=" + accountId);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).post(
        entity(request, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    assertNotNull(response.getResource());
    assertThat(response.getResource()).isTrue();
  }

  private List<Role> getAccountAdminRoles() {
    return wingsPersistence
        .query(Role.class,
            aPageRequest()
                .addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                .addFilter("accountId", Operator.EQ, accountId)
                .withLimit("2")
                .build())
        .getResponse();
  }

  private User completeTrialUserInviteAndSignin(String inviteId) {
    WebTarget target = client.target(API_BASE + "/users/invites/trial/" + inviteId + "/new-signin");
    RestResponse<User> response =
        target.request().put(entity(inviteId, APPLICATION_JSON), new GenericType<RestResponse<User>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    assertNotNull(response.getResource());

    return response.getResource();
  }

  private UserInvite inviteUser(String accountId, String name, String email) {
    String userInviteJson =
        "{\"name\":\"" + name + "\", \"accountId\":\"" + accountId + "\", \"emails\":[\"" + email + "\"]}";
    WebTarget target = client.target(API_BASE + "/users/invites?accountId=" + accountId);
    RestResponse<List<UserInvite>> response = getRequestBuilderWithAuthHeader(target).post(
        entity(userInviteJson, APPLICATION_JSON), new GenericType<RestResponse<List<UserInvite>>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    assertNotNull(response.getResource());
    assertEquals(1, response.getResource().size());

    return response.getResource().get(0);
  }

  private User completeUserInviteAndSignIn(String accountId, String inviteId, UserInvite userInvite) {
    WebTarget target = client.target(API_BASE + "/users/invites/" + inviteId + "?accountId=" + accountId);
    RestResponse<User> response =
        target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<User>>() {});
    assertThat(response.getResponseMessages()).isEmpty();
    assertNotNull(response.getResource());

    return response.getResource();
  }
}
