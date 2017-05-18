package software.wings.integration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.io.ByteStreams;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 4/24/17.
 */
public class UserServiceIntegrationTest extends BaseIntegrationTest {
  @Test
  public void testBlankEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    Assert.assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    Assert.assertFalse(restResponse.getResource());
  }

  @Test
  public void testInvalidEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    Assert.assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    Assert.assertFalse(restResponse.getResource());
  }

  @Test
  public void testDomainNotAllowed() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz@gmail.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    Assert.assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, responseMessage.getCode());
    Assert.assertFalse(restResponse.getResource());
  }

  @Test
  public void testUserExists() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=admin@wings.software");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    Assert.assertEquals(ErrorCode.USER_ALREADY_REGISTERED, responseMessage.getCode());
    Assert.assertFalse(restResponse.getResource());
  }

  @Test
  public void testValidEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=raghu@wings.software");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertTrue(restResponse.getResource());
  }

  @Test
  public void testValidEmailWithSpace() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=%20raghu@wings.software%20");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertTrue(restResponse.getResource());
  }

  @Test
  public void testSignupSuccess() throws IOException {
    final String name = "Raghu Singh";
    final String email = "abc@wings.software";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = "some account";
    final String companyName = "some company";
    WebTarget target = client.target(API_BASE + "/users");
    RestResponse<User> response = target.request().post(
        Entity.entity(
            anUser()
                .withName(name)
                .withEmail(email)
                .withPassword(pwd)
                .withRoles(
                    wingsPersistence
                        .query(Role.class,
                            aPageRequest()
                                .addFilter(
                                    aSearchFilter().withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                .build())
                        .getResponse())
                .withAccountName(accountName)
                .withCompanyName(companyName)
                .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<User>>() {});
    Assert.assertEquals(0, response.getResponseMessages().size());
    final User savedUser = response.getResource();
    Assert.assertEquals(name, savedUser.getName());
    Assert.assertEquals(email, savedUser.getEmail());
    Assert.assertNull(savedUser.getPassword());
    Assert.assertEquals(1, savedUser.getAccounts().size());
    Assert.assertEquals(accountName, savedUser.getAccounts().get(0).getAccountName());
    Assert.assertEquals(companyName, savedUser.getAccounts().get(0).getCompanyName());
  }

  @Test
  public void testSignupSuccessWithSpaces() throws IOException {
    final String name = "  Brad  Pitt    ";
    final String email = "xyz@wings.software";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    RestResponse<User> response = null;
    try {
      response = target.request().post(
          Entity.entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest()
                                         .addFilter(aSearchFilter()
                                                        .withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                                                        .build())
                                         .build())
                                 .getResponse())
                  .withAccountName(accountName)
                  .withCompanyName(companyName)
                  .build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
    } catch (BadRequestException e) {
      System.out.println(new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity())));
      Assert.fail();
    }
    Assert.assertEquals(0, response.getResponseMessages().size());
    final User savedUser = response.getResource();
    Assert.assertEquals(name.trim(), savedUser.getName());
    Assert.assertEquals(email.trim(), savedUser.getEmail());
    Assert.assertNull(savedUser.getPassword());
    Assert.assertEquals(1, savedUser.getAccounts().size());
    Assert.assertEquals(accountName.trim(), savedUser.getAccounts().get(0).getAccountName());
    Assert.assertEquals(companyName.trim(), savedUser.getAccounts().get(0).getCompanyName());
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
          Entity.entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest()
                                         .addFilter(aSearchFilter()
                                                        .withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                                                        .build())
                                         .build())
                                 .getResponse())
                  .withAccountName(accountName)
                  .withCompanyName(companyName)
                  .build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      Assert.assertEquals(1, restResponse.getResponseMessages().size());
      Assert.assertEquals(ErrorCode.INVALID_ARGUMENT, restResponse.getResponseMessages().get(0).getCode());
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
          Entity.entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest()
                                         .addFilter(aSearchFilter()
                                                        .withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                                                        .build())
                                         .build())
                                 .getResponse())
                  .withAccountName(accountName)
                  .withCompanyName(companyName)
                  .build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      Assert.assertEquals(1, restResponse.getResponseMessages().size());
      Assert.assertEquals(ErrorCode.INVALID_EMAIL, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  public void testSignupBadEmailDomain() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@gmail.com";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(
          Entity.entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest()
                                         .addFilter(aSearchFilter()
                                                        .withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                                                        .build())
                                         .build())
                                 .getResponse())
                  .withAccountName(accountName)
                  .withCompanyName(companyName)
                  .build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      Assert.assertEquals(1, restResponse.getResponseMessages().size());
      Assert.assertEquals(
          ErrorCode.DOMAIN_NOT_ALLOWED_TO_REGISTER, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  public void testSignupEmailExists() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "admin@wings.software";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(
          Entity.entity(
              anUser()
                  .withName(name)
                  .withEmail(email)
                  .withPassword(pwd)
                  .withRoles(wingsPersistence
                                 .query(Role.class,
                                     aPageRequest()
                                         .addFilter(aSearchFilter()
                                                        .withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                                                        .build())
                                         .build())
                                 .getResponse())
                  .withAccountName(accountName)
                  .withCompanyName(companyName)
                  .build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with existing email");
    } catch (ClientErrorException e) {
      Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      Assert.assertEquals(1, restResponse.getResponseMessages().size());
      Assert.assertEquals(ErrorCode.USER_ALREADY_REGISTERED, restResponse.getResponseMessages().get(0).getCode());
    }
  }
}
