package software.wings.integration;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
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
import io.harness.eraro.ErrorCode;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Account;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AccountService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
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

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  public void testBlankEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  public void testInvalidEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  public void testDomainNotAllowed() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz@some-domain.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  public void testUserExists() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=admin@harness.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_ALREADY_REGISTERED, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  public void testValidEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=" + validEmail);
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());
  }

  @Test
  public void testValidEmailWithSpace() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=%20" + validEmail + "%20");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());
  }

  @Test
  public void testSignupSuccess() throws IOException {
    final String name = "Raghu Singh";
    final String email = "abc" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = "some account" + System.currentTimeMillis();
    final String companyName = "some company" + System.currentTimeMillis();
    WebTarget target = client.target(API_BASE + "/users");
    RestResponse<User> response = target.request().post(
        entity(anUser()
                   .withName(name)
                   .withEmail(email)
                   .withPassword(pwd)
                   .withRoles(wingsPersistence
                                  .query(Role.class,
                                      aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                  .getResponse())
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
  }

  @Test
  public void testSignupSuccessWithSpaces() throws IOException {
    final String name = "  Brad  Pitt    ";
    final String email = "xyz" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   " + System.currentTimeMillis();
    final String companyName = "  star   wars    enterprise   " + System.currentTimeMillis();
    WebTarget target = client.target(API_BASE + "/users");

    RestResponse<User> response = null;
    try {
      response = target.request().post(
          entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                 .getResponse())
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
  public void testSignupEmailWithSpace() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "  xyz@wings  ";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(
          entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                 .getResponse())
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
  public void testSignupBadEmail() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@wings";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(
          entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                 .getResponse())
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
  public void testSignupBadEmailDomain() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@some-email.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(
          entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                 .getResponse())
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
      assertEquals(ErrorCode.DOMAIN_NOT_ALLOWED_TO_REGISTER, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  public void testSignupEmailExists() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "admin@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(
          entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                 .getResponse())
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
}
