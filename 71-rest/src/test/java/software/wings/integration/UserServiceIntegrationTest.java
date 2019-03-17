package software.wings.integration;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.User.Builder.anUser;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.IntegrationTests;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.common.Constants;
import software.wings.resources.UserResource.ResendInvitationEmailRequest;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 4/24/17.
 */
public class UserServiceIntegrationTest extends BaseIntegrationTest {
  private final String validEmail = "raghu" + System.currentTimeMillis() + "@wings.software";
  @Inject private AccountService accountService;
  @Inject private UserService userService;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testBlankEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testInvalidEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDomainNotAllowed() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz@some-domain.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUserExists() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=admin@harness.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_ALREADY_REGISTERED, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=" + validEmail);
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidEmailWithSpace() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=%20" + validEmail + "%20");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testTrialSignupSuccess() {
    final String name = "Mark Lu";
    final String pwd = "somepwd";
    final String email = "mark" + System.currentTimeMillis() + "@harness.io";

    WebTarget target = client.target(API_BASE + "/users/trial");
    // Trial signup with just one email address, nothing else.
    RestResponse<Boolean> response =
        target.request().post(entity(email, TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertTrue(response.getResource());

    // Trial signup again using the same email should succeed. A new verification email will be sent.
    response = target.request().post(entity(email, TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertTrue(response.getResource());

    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class).filter("email", email).get();
    assertNotNull(userInvite);
    assertFalse(userInvite.isCompleted());
    String inviteId = userInvite.getUuid();
    assertNotNull(inviteId);

    // Preparing for completing the invitation.
    final String accountName = "Harness_" + System.currentTimeMillis();
    final String companyName = "Harness_" + System.currentTimeMillis();
    userInvite.setName(name);
    userInvite.setAgreement(true);
    userInvite.setPassword(pwd.toCharArray());

    // Complete the trial invitation.
    userInvite = completeTrialUserInvite(inviteId, userInvite, accountName, companyName);
    assertTrue(userInvite.isCompleted());
    assertEquals(SourceType.TRIAL, userInvite.getSource().getType());
    String accountId = userInvite.getAccountId();
    assertNotNull(userInvite.getAccountId());
    assertTrue(userInvite.isAgreement());

    // Verify the account get created.
    Account account = accountService.get(accountId);
    assertNotNull(account);

    // Verify the trial user is created, assigned to proper account and with the account admin roles.
    User savedUser = wingsPersistence.createQuery(User.class).filter("email", email).get();
    assertNotNull(savedUser);
    assertTrue(savedUser.isEmailVerified());
    assertEquals(1, savedUser.getAccounts().size());

    // Trial signup a few more time using the same email will trigger the rejection, and the singup result will be
    // false.
    for (int i = 0; i < UserServiceImpl.REGISTRATION_SPAM_THRESHOLD; i++) {
      response = target.request().post(entity(email, TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    }
    assertEquals(0, response.getResponseMessages().size());
    assertFalse(response.getResource());

    // Delete the user just created as a cleanup
    userService.delete(savedUser.getAccounts().get(0).getUuid(), savedUser.getUuid());

    // Verify user is deleted
    assertNull(userService.getUserByEmail(email));
    // Verify user invite is deleted
    assertNull(wingsPersistence.createQuery(UserInvite.class).filter("email", email).get());
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
    assertEquals(0, response.getResponseMessages().size());
    final User savedUser = response.getResource();
    assertEquals(name, savedUser.getName());
    assertEquals(email, savedUser.getEmail());
    assertNull(savedUser.getPassword());
    assertEquals(1, savedUser.getAccounts().size());
    assertEquals(accountName, savedUser.getAccounts().get(0).getAccountName());
    assertEquals(companyName, savedUser.getAccounts().get(0).getCompanyName());

    // Delete the user just created as a cleanup
    userService.delete(savedUser.getAccounts().get(0).getUuid(), savedUser.getUuid());

    // Verify user is deleted
    assertNull(userService.getUserByEmail(email));
    // Verify user invite is deleted
    assertNull(wingsPersistence.createQuery(UserInvite.class).filter("email", email).get());
  }

  @Test
  @Owner(emails = "mark.lu@harness.io", intermittent = true)
  @Category(UnitTests.class)
  public void testUserInviteSignupAndSignInSuccess() {
    final String name = "Mark Lu";
    final String email = "abc" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    UserInvite userInvite = inviteUser(accountId, name, email);
    assertNotNull(userInvite.getUuid());
    assertFalse(userInvite.isCompleted());
    assertEquals(email, userInvite.getEmail());

    userInvite.setName(name);
    userInvite.setPassword(pwd);
    userInvite.setAgreement(true);
    completeUserInviteAndSignIn(accountId, userInvite.getUuid(), userInvite);

    UserInvite retrievedUserInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    assertTrue(retrievedUserInvite.isCompleted());

    // Delete the user just created as a cleanup
    User user = userService.getUserByEmail(email);
    userService.delete(accountId, user.getUuid());
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
    assertEquals(0, response.getResponseMessages().size());
    final User savedUser = response.getResource();
    assertEquals(name.trim(), savedUser.getName());
    assertEquals(email.trim(), savedUser.getEmail());
    assertNull(savedUser.getPassword());
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
  public void testSignupBadEmailDomain() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@some-email.io";
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
    } catch (ClientErrorException e) {
      assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, restResponse.getResponseMessages().get(0).getCode());
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
    User user = wingsPersistence.createQuery(User.class).filter("email", "admin@harness.io").get();

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
                                               .licenseUnits(Constants.DEFAULT_PAID_LICENSE_UNITS)
                                               .build())

                          .build();

    assertFalse(accountService.exists(account.getAccountName()));
    assertNull(accountService.getByName(account.getCompanyName()));

    WebTarget target = client.target(API_BASE + "/users/account");
    RestResponse<Account> response = getRequestBuilderWithAuthHeader(target).post(
        entity(account, APPLICATION_JSON), new GenericType<RestResponse<Account>>() {});

    assertNotNull(response.getResource());
    assertTrue(accountService.exists(account.getAccountName()));
    assertNotNull(accountService.getByName(account.getCompanyName()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testResendInvitationAndCompleteInvitation() {
    Account adminAccount = wingsPersistence.createQuery(Account.class).filter("accountName", defaultAccountName).get();
    String accountId = adminAccount.getUuid();

    ResendInvitationEmailRequest request = new ResendInvitationEmailRequest();
    request.setEmail(adminUserEmail);

    WebTarget target = client.target(API_BASE + "/users/resend-invitation-email?accountId=" + accountId);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).post(
        entity(request, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());
    assertTrue(response.getResource());
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

  private UserInvite completeTrialUserInvite(
      String inviteId, UserInvite userInvite, String accountName, String companyName) {
    WebTarget target = client.target(
        API_BASE + "/users/invites/trial/" + inviteId + "?account=" + accountName + "&company=" + companyName);
    RestResponse<UserInvite> response =
        target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<UserInvite>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());

    return response.getResource();
  }

  private UserInvite inviteUser(String accountId, String name, String email) {
    String userInviteJson =
        "{\"name\":\"" + name + "\", \"accountId\":\"" + accountId + "\", \"emails\":[\"" + email + "\"]}";
    WebTarget target = client.target(API_BASE + "/users/invites?accountId=" + accountId);
    RestResponse<List<UserInvite>> response = getRequestBuilderWithAuthHeader(target).post(
        entity(userInviteJson, APPLICATION_JSON), new GenericType<RestResponse<List<UserInvite>>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());
    assertEquals(1, response.getResource().size());

    return response.getResource().get(0);
  }

  private User completeUserInviteAndSignIn(String accountId, String inviteId, UserInvite userInvite) {
    WebTarget target = client.target(API_BASE + "/users/invites/" + inviteId + "?accountId=" + accountId);
    RestResponse<User> response =
        target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<User>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());

    return response.getResource();
  }
}
