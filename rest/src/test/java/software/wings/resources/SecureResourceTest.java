package software.wings.resources;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.AccountRole.AccountRoleBuilder.anAccountRole;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ApplicationRole.ApplicationRoleBuilder.anApplicationRole;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.EnvironmentRole.EnvironmentRoleBuilder.anEnvironmentRole;
import static software.wings.beans.Permission.Builder.aPermission;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.security.PermissionAttribute.Action.ALL;
import static software.wings.security.PermissionAttribute.Action.CREATE;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionScope.APP;
import static software.wings.security.PermissionAttribute.PermissionScope.ENV;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.jexl3.JxltEngine.Exception;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.AuthRuleFilter;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.AuthServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;
import software.wings.utils.ResourceTestRule;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 8/31/16.
 */
public class SecureResourceTest {
  /**
   * The constant TOKEN_EXPIRY_IN_MILLIS.
   */
  public static final long TOKEN_EXPIRY_IN_MILLIS = 86400000L;
  /**
   * The constant VALID_TOKEN.
   */
  public static final String VALID_TOKEN = "VALID_TOKEN";
  /**
   * The constant ENTITY.
   */
  public static final Entity<Base> ENTITY = Entity.entity(new Base(), APPLICATION_JSON);

  private static AuditService auditService = mock(AuditService.class);
  private static AuditHelper auditHelper = mock(AuditHelper.class);
  private static GenericDbCache genericDbCache = mock(GenericDbCache.class);
  private static AccountService accountService = mock(AccountService.class);
  private static WingsPersistence wingsPersistence = mock(WingsPersistence.class);
  private static CacheHelper cacheHelper = mock(CacheHelper.class);

  private static AppService appService = mock(AppService.class);
  private static UserService userService = mock(UserService.class);
  private static MainConfiguration configuration = mock(MainConfiguration.class);

  private static AuthService authService =
      new AuthServiceImpl(genericDbCache, accountService, wingsPersistence, userService, cacheHelper, configuration);

  private static AuthRuleFilter authRuleFilter =
      new AuthRuleFilter(auditService, auditHelper, authService, appService, userService);

  Cache<String, User> cache = Mockito.mock(Cache.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new SecureResource()).addProvider(authRuleFilter).build();

  private String accountKey = "2f6b0988b6fb3370073c3d0505baee59";

  private final Role appAllResourceReadActionRole =
      aRole()
          .withAppId(GLOBAL_APP_ID)
          .withName(ROLE_NAME)
          .withUuid(ROLE_ID)
          .withAccountId(ACCOUNT_ID)
          .withPermissions(asList(aPermission()
                                      .withAppId(APP_ID)
                                      .withEnvId(ENV_ID)
                                      .withPermissionScope(APP)
                                      .withResourceType(ResourceType.APPLICATION)
                                      .withAction(READ)
                                      .build()))
          .build();
  private final Role appAllResourceCreateActionRole =
      aRole()
          .withAppId(GLOBAL_APP_ID)
          .withName(ROLE_NAME)
          .withUuid(ROLE_ID)
          .withAccountId(ACCOUNT_ID)
          .withPermissions(asList(aPermission()
                                      .withAppId(APP_ID)
                                      .withEnvId(ENV_ID)
                                      .withPermissionScope(APP)
                                      .withResourceType(ResourceType.APPLICATION)
                                      .withAction(CREATE)
                                      .build()))
          .build();
  private final Role appAllResourceWriteActionRole =
      aRole()
          .withAppId(GLOBAL_APP_ID)
          .withName(ROLE_NAME)
          .withUuid(ROLE_ID)
          .withAccountId(ACCOUNT_ID)
          .withPermissions(asList(aPermission()
                                      .withAppId(APP_ID)
                                      .withEnvId(ENV_ID)
                                      .withPermissionScope(APP)
                                      .withResourceType(ResourceType.APPLICATION)
                                      .withAction(UPDATE)
                                      .build()))
          .build();

  private final Role envAllResourceReadActionRole =
      aRole()
          .withAppId(GLOBAL_APP_ID)
          .withName(ROLE_NAME)
          .withUuid(ROLE_ID)
          .withAccountId(ACCOUNT_ID)
          .withPermissions(asList(aPermission()
                                      .withAppId(APP_ID)
                                      .withEnvId(ENV_ID)
                                      .withPermissionScope(ENV)
                                      .withResourceType(ResourceType.ENVIRONMENT)
                                      .withAction(READ)
                                      .build()))
          .build();
  private final Role envAllResourceWriteActionRole =
      aRole()
          .withAppId(GLOBAL_APP_ID)
          .withName(ROLE_NAME)
          .withUuid(ROLE_ID)
          .withAccountId(ACCOUNT_ID)
          .withPermissions(asList(aPermission()
                                      .withAppId(APP_ID)
                                      .withEnvId(ENV_ID)
                                      .withPermissionScope(ENV)
                                      .withResourceType(ResourceType.ENVIRONMENT)
                                      .withAction(UPDATE)
                                      .build()))
          .build();

  private final Account account = Account.Builder.anAccount().withUuid(ACCOUNT_ID).build();
  private User user =
      anUser()
          .withUuid(USER_ID)
          .withAppId(GLOBAL_APP_ID)
          .withEmail(USER_EMAIL)
          .withName(USER_NAME)
          .withPassword(PASSWORD)
          .withAccounts(Lists.newArrayList(account))
          .withRoles(asList(aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.ACCOUNT_ADMIN).build()))
          .build();

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(cacheHelper.getUserCache()).thenReturn(cache);
    when(cache.get(USER_ID)).thenReturn(user);

    when(genericDbCache.get(AuthToken.class, VALID_TOKEN)).thenReturn(new AuthToken(USER_ID, TOKEN_EXPIRY_IN_MILLIS));
    when(genericDbCache.get(Account.class, ACCOUNT_ID))
        .thenReturn(anAccount().withUuid(ACCOUNT_ID).withAccountKey(accountKey).build());
    when(genericDbCache.get(Application.class, APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId(ACCOUNT_ID).build());
    when(genericDbCache.get(Environment.class, ENV_ID))
        .thenReturn(
            anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withEnvironmentType(EnvironmentType.NON_PROD).build());
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withUuid(ACCOUNT_ID).withAccountKey(accountKey).build());
    when(appService.get(APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId(ACCOUNT_ID).build());
    when(userService.getUserAccountRole(USER_ID, ACCOUNT_ID))
        .thenReturn(anAccountRole()
                        .withAccountId(ACCOUNT_ID)
                        .withApplicationRoles(ImmutableList.of(anApplicationRole().withAppId(APP_ID).build()))
                        .build());
    when(userService.getUserApplicationRole(USER_ID, APP_ID))
        .thenReturn(anApplicationRole()
                        .withAppId(APP_ID)
                        .withEnvironmentRoles(ImmutableList.of(anEnvironmentRole()
                                                                   .withEnvId(ENV_ID)
                                                                   .withEnvName(ENV_NAME)
                                                                   .withEnvironmentType(EnvironmentType.PROD)
                                                                   .build()))
                        .build());
  }

  /**
   * Tear down.
   *
   * @throws Exception the exception
   */
  @After
  public void tearDown() throws Exception {
    UserThreadLocal.unset();
  }

  /**
   * Should get public resource without authorization.
   */
  @Test
  public void shouldGetPublicResourceWithoutAuthorization() {
    Response response = resources.client().target("/secure-resources/publicApiAuthTokenNotRequired").request().get();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  /**
   * Should deny access for non public resource without valid token.
   */
  @Test
  public void shouldDenyAccessForNonPublicResourceWithoutValidToken() {
    Assertions.assertThatThrownBy(() -> resources.client().target("/secure-resources/NonPublicApi").request().get())
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCode.INVALID_TOKEN.name());
  }

  /**
   * Should require authorization by default for non public resource.
   */
  @Test
  public void shouldRequireAuthorizationByDefaultForNonPublicResource() {
    RestResponse<User> response = resources.client()
                                      .target("/secure-resources/NonPublicApi?appId=" + APP_ID)
                                      .request()
                                      .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                      .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  /**
   * Should authorize app scope resource read request for user with required permission.
   */
  @Test
  public void shouldAuthorizeAppScopeResourceReadRequestForUserWithRequiredPermission() {
    user.setRoles(asList(appAllResourceReadActionRole));

    RestResponse<User> response = resources.client()
                                      .target("/secure-resources/appResourceReadActionOnAppScope?appId=APP_ID")
                                      .request()
                                      .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                      .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  /**
   * Should authorize app scope resource write request for user with required permission.
   */
  @Test
  public void shouldAuthorizeAppScopeResourceWriteRequestForUserWithRequiredPermission() {
    user.setRoles(asList(appAllResourceCreateActionRole));

    RestResponse<User> response = resources.client()
                                      .target("/secure-resources/appResourceWriteActionOnAppScope?appId=APP_ID")
                                      .request()
                                      .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                      .post(ENTITY, new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  /**
   * Should deny app scope resource read request for user without required permission.
   */
  @Test
  public void shouldDenyAppScopeResourceReadRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(()
                                -> resources.client()
                                       .target("/secure-resources/appResourceReadActionOnAppScope?appId=APP_ID")
                                       .request()
                                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                       .get())
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCode.ACCESS_DENIED.name());
  }

  /**
   * Should deny app scope resource write request for user without required permission.
   */
  @Test
  public void shouldDenyAppScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(()
                                -> resources.client()
                                       .target("/secure-resources/appResourceWriteActionOnAppScope?appId=APP_ID")
                                       .request()
                                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                       .post(ENTITY))
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCode.ACCESS_DENIED.name());
  }

  /**
   * Should authorize env scope resource read request for user with required permission.
   */
  @Test
  public void shouldAuthorizeEnvScopeResourceReadRequestForUserWithRequiredPermission() {
    user.setRoles(asList(envAllResourceReadActionRole));

    RestResponse<User> response =
        resources.client()
            .target("/secure-resources/envResourceReadActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
            .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  /**
   * Should authorize env scope resource write request for user with required permission.
   */
  @Test
  public void shouldAuthorizeEnvScopeResourceWriteRequestForUserWithRequiredPermission() {
    user.setRoles(asList(appAllResourceCreateActionRole));

    RestResponse<User> response =
        resources.client()
            .target("/secure-resources/envResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
            .post(ENTITY, new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  /**
   * Should deny env scope resource read request for user without required permission.
   */
  @Test
  public void shouldDenyEnvScopeResourceReadRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(
            ()
                -> resources.client()
                       .target("/secure-resources/envResourceReadActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
                       .request()
                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                       .get())
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCode.ACCESS_DENIED.name());
  }

  /**
   * Should deny env scope resource write request for user without required permission.
   */
  @Test
  public void shouldDenyEnvScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(
            ()
                -> resources.client()
                       .target("/secure-resources/envResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
                       .request()
                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                       .post(ENTITY))
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCode.ACCESS_DENIED.name());
  }

  /**
   * Should deny env scope release resource read request for user without required permission.
   */
  @Test
  public void shouldDenyEnvScopeReleaseResourceReadRequestForUserWithoutRequiredPermission() {
    Role envDeploymentResourceALLActionRole = aRole()
                                                  .withAppId(GLOBAL_APP_ID)
                                                  .withName(ROLE_NAME)
                                                  .withUuid(ROLE_ID)
                                                  .withPermissions(asList(aPermission()
                                                                              .withAppId(APP_ID)
                                                                              .withEnvId(ENV_ID)
                                                                              .withPermissionScope(ENV)
                                                                              .withResourceType(ResourceType.DEPLOYMENT)
                                                                              .withAction(ALL)
                                                                              .build()))
                                                  .build();
    user.setRoles(asList(envDeploymentResourceALLActionRole));

    Assertions
        .assertThatThrownBy(
            ()
                -> resources.client()
                       .target("/secure-resources/envResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
                       .request()
                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                       .post(ENTITY))
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  public void shouldValidateTokenForDelegate() {
    RestResponse<String> response = resources.client()
                                        .target("/secure-resources/delegateAuth?accountId=ACCOUNT_ID")
                                        .request()
                                        .header(HttpHeaders.AUTHORIZATION, "Delegate " + getDelegateToken(accountKey))
                                        .get(new GenericType<RestResponse<String>>() {});
    assertThat(response.getResource()).isEqualTo("test");
  }

  private String getDelegateToken(String accountSecret) {
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issuer("localhost")
                                 .subject(ACCOUNT_ID)
                                 .audience("manager")
                                 .expirationTime(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
                                 .notBeforeTime(new Date())
                                 .issueTime(new Date())
                                 .jwtID(UUID.randomUUID().toString())
                                 .build();

    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM);
    EncryptedJWT jwt = new EncryptedJWT(header, jwtClaims);
    DirectEncrypter directEncrypter = null;
    byte[] encodedKey = new byte[0];
    try {
      encodedKey = Hex.decodeHex(accountSecret.toCharArray());
    } catch (DecoderException e) {
      e.printStackTrace();
    }
    try {
      directEncrypter = new DirectEncrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      e.printStackTrace();
      return null;
    }

    try {
      jwt.encrypt(directEncrypter);
    } catch (JOSEException e) {
      e.printStackTrace();
      return null;
    }
    return jwt.serialize();
  }
}
