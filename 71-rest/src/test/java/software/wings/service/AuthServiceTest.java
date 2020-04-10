package software.wings.service;

import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.VIKAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.security.TokenGenerator;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mongodb.morphia.AdvancedDatastore;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.dl.GenericDbCache;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.CacheManager;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.cache.Cache;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Created by anubhaw on 8/31/16.
 */
public class AuthServiceTest extends WingsBaseTest {
  private final String VALID_TOKEN = "VALID_TOKEN";
  private final String INVALID_TOKEN = "INVALID_TOKEN";
  private final String EXPIRED_TOKEN = "EXPIRED_TOKEN";
  private final String NOT_AVAILABLE_TOKEN = "NOT_AVAILABLE_TOKEN";
  private final String AUTH_SECRET = "AUTH_SECRET";

  @Mock private GenericDbCache cache;
  @Mock private static CacheManager cacheManager;
  @Mock private Cache<String, User> userCache;
  @Mock private Cache<String, AuthToken> authTokenCache;
  @Mock private HPersistence persistence;
  @Mock private AdvancedDatastore advancedDatastore;

  @Mock private AccountService accountService;
  @Mock private SegmentHandler segmentHandler;
  @Mock FeatureFlagService featureFlagService;
  @Mock PortalConfig portalConfig;
  @Inject MainConfiguration mainConfiguration;
  @Inject @InjectMocks private AuthService authService;
  private Builder userBuilder = anUser().appId(APP_ID).email(USER_EMAIL).name(USER_NAME).password(PASSWORD);
  private String accountKey = "2f6b0988b6fb3370073c3d0505baee59";

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    on(mainConfiguration).set("portal", portalConfig);
    when(cacheManager.getUserCache()).thenReturn(userCache);
    when(userCache.get(USER_ID)).thenReturn(User.Builder.anUser().uuid(USER_ID).build());

    when(cacheManager.getAuthTokenCache()).thenReturn(authTokenCache);
    when(authTokenCache.get(VALID_TOKEN)).thenReturn(new AuthToken(ACCOUNT_ID, USER_ID, 86400000L));
    when(authTokenCache.get(EXPIRED_TOKEN)).thenReturn(new AuthToken(ACCOUNT_ID, USER_ID, 0L));

    when(cache.get(Application.class, APP_ID)).thenReturn(anApplication().uuid(APP_ID).appId(APP_ID).build());
    when(cache.get(Environment.class, ENV_ID))
        .thenReturn(anEnvironment().appId(APP_ID).uuid(ENV_ID).environmentType(EnvironmentType.NON_PROD).build());
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withUuid(ACCOUNT_ID).withAccountKey(accountKey).build());
    when(cache.get(Account.class, ACCOUNT_ID))
        .thenReturn(anAccount().withUuid(ACCOUNT_ID).withAccountKey(accountKey).build());
    when(portalConfig.getJwtAuthSecret()).thenReturn(AUTH_SECRET);

    when(persistence.getDatastore(AuthToken.class)).thenReturn(advancedDatastore);
  }

  /**
   * Test whether auth token is fetched from db if its not available in cache
   */
  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testAuthTokenNotAvailableInCache() {
    AuthToken authTokenInDB = new AuthToken(ACCOUNT_ID, USER_ID, 86400000L);
    when(advancedDatastore.get(AuthToken.class, NOT_AVAILABLE_TOKEN)).thenReturn(authTokenInDB);
    AuthToken authToken = authService.validateToken(NOT_AVAILABLE_TOKEN);
    assertThat(authToken).isNotNull().isInstanceOf(AuthToken.class);
    assertThat(authToken).isEqualTo(authTokenInDB);
    verify(advancedDatastore, times(1)).get(AuthToken.class, NOT_AVAILABLE_TOKEN);
  }

  /**
   * Should validate valid token.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldValidateValidToken() {
    AuthToken authToken = authService.validateToken(VALID_TOKEN);
    assertThat(authToken).isNotNull().isInstanceOf(AuthToken.class);
  }

  /**
   * Should throw invalid token exception for invalid token.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowInvalidTokenExceptionForInvalidToken() {
    assertThatThrownBy(() -> authService.validateToken(INVALID_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_TOKEN.name());
  }

  /**
   * Should throw expired token exception for expired token.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowExpiredTokenExceptionForExpiredToken() {
    assertThatThrownBy(() -> authService.validateToken(EXPIRED_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.EXPIRED_TOKEN.name());
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldAuthorizeWithAccountAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.ACCOUNT_ADMIN).build();
    User user = userBuilder.but().roles(asList(role)).build();
    String appId = null;
    try {
      authService.authorize(
          ACCOUNT_ID, appId, null, user, asList(new PermissionAttribute(ResourceType.USER, Action.READ)), null);
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDenyWithoutAccountAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.APPLICATION_ADMIN).build();
    role.onLoad();
    User user = userBuilder.but().roles(asList(role)).build();
    String appId = null;
    assertThatThrownBy(()
                           -> authService.authorize(ACCOUNT_ID, appId, null, user,
                               asList(new PermissionAttribute(ResourceType.USER, Action.READ)), null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldAuthorizeWithAppAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.APPLICATION_ADMIN).withAppId(APP_ID).build();
    role.onLoad();
    User user = userBuilder.but().roles(asList(role)).build();
    authService.authorize(
        ACCOUNT_ID, APP_ID, null, user, asList(new PermissionAttribute(ResourceType.ARTIFACT, Action.UPDATE)), null);
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldAuthorizeReadWithEnvAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.NON_PROD_SUPPORT).withAppId(APP_ID).build();
    role.onLoad();
    User user = userBuilder.but().roles(asList(role)).build();
    authService.authorize(
        ACCOUNT_ID, APP_ID, ENV_ID, user, asList(new PermissionAttribute(ResourceType.APPLICATION, Action.READ)), null);
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDenyWithDiffAppAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.APPLICATION_ADMIN).withAppId("APP_ID2").build();
    role.onLoad();
    User user = userBuilder.but().roles(asList(role)).build();
    assertThatThrownBy(()
                           -> authService.authorize(ACCOUNT_ID, APP_ID, null, user,
                               asList(new PermissionAttribute(ResourceType.APPLICATION, Action.READ)), null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDenyWriteWithEnvAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.NON_PROD_SUPPORT).withAppId(APP_ID).build();
    role.onLoad();
    User user = userBuilder.but().roles(asList(role)).build();
    assertThatThrownBy(()
                           -> authService.authorize(ACCOUNT_ID, APP_ID, ENV_ID, user,
                               asList(new PermissionAttribute(ResourceType.APPLICATION, Action.UPDATE)), null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldValidateDelegateToken() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    authService.validateDelegateToken(ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname"));
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldThrowDenyAccessWhenAccountIdNotFoundForDelegate() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    assertThatThrownBy(()
                           -> authService.validateDelegateToken(
                               ACCOUNT_ID + "1", tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowThrowInavlidTokenForDelegate() {
    assertThatThrownBy(() -> authService.validateDelegateToken(ACCOUNT_ID, "Dummy"))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_TOKEN.name());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenUnableToDecryptToken() {
    assertThatThrownBy(() -> authService.validateDelegateToken(ACCOUNT_ID, getDelegateToken()))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_TOKEN.name());
  }

  private String getDelegateToken() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, Hex.encodeHexString(encoded));
    return tokenGenerator.getToken("https", "localhost", 9090, "hostname");
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testGenerateBearerTokenWithJWTToken() throws UnsupportedEncodingException {
    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(true);
    Account mockAccount = Account.Builder.anAccount().withAccountKey("TestAccount").build();
    User mockUser = getMockUser(mockAccount);
    mockUser.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    mockUser.setUuid("kmpySmUISimoRrJL6NL73w");
    when(userCache.get(USER_ID)).thenReturn(mockUser);
    User user = authService.generateBearerTokenForUser(mockUser);
    assertThat(user.getToken().length()).isGreaterThan(32);

    Algorithm algorithm = Algorithm.HMAC256(AUTH_SECRET);
    JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
    String authTokenId = JWT.decode(user.getToken()).getClaim("authToken").asString();

    String tokenString = user.getToken();
    AuthToken authToken = new AuthToken(ACCOUNT_ID, USER_ID, 8640000L);
    authToken.setJwtToken(user.getToken());
    when(authTokenCache.get(authTokenId)).thenReturn(authToken);
    assertThat(authService.validateToken(tokenString)).isEqualTo(authToken);

    try {
      authService.validateToken(tokenString + "FakeToken");
      fail("WingsException should have been thrown");
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIAL.name());
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testGenerateBearerTokenWithoutJWTToken() {
    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(false);
    Account mockAccount = Account.Builder.anAccount().withAccountKey("TestAccount").build();
    User mockUser = getMockUser(mockAccount);
    mockUser.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    mockUser.setUuid("kmpySmUISimoRrJL6NL73w");
    when(userCache.get(USER_ID)).thenReturn(mockUser);
    User user = authService.generateBearerTokenForUser(mockUser);
    AuthToken authToken = new AuthToken(ACCOUNT_ID, USER_ID, 8640000L);
    JWT jwt = JWT.decode(user.getToken());
    String authTokenUuid = jwt.getClaim("authToken").asString();
    when(cache.get(Matchers.any(), Matchers.matches(authTokenUuid))).thenReturn(authToken);
    when(authTokenCache.get(authTokenUuid)).thenReturn(authToken);
    assertThat(user.getToken().length()).isGreaterThan(32);
    authService.validateToken(user.getToken());
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldSendSegmentTrackEvent() throws IllegalAccessException {
    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(false);
    Account mockAccount = Account.Builder.anAccount().withAccountKey("TestAccount").withUuid(ACCOUNT_ID).build();
    User mockUser = getMockUser(mockAccount);
    mockUser.setLastAccountId(ACCOUNT_ID);
    when(userCache.get(USER_ID)).thenReturn(mockUser);

    FieldUtils.writeField(authService, "segmentHandler", segmentHandler, true);
    authService.generateBearerTokenForUser(mockUser);
    try {
      Thread.sleep(10000);
      verify(segmentHandler, times(1))
          .reportTrackEvent(any(Account.class), anyString(), any(User.class), anyMap(), anyMap());
    } catch (InterruptedException | URISyntaxException e) {
      throw new InvalidRequestException(e.getMessage());
    }
  }

  private User getMockUser(Account mockAccount) {
    return Builder.anUser()
        .uuid(USER_ID)
        .name("TestUser")
        .email("admin@abcd.io")
        .appId("TestApp")
        .accounts(Arrays.asList(mockAccount))
        .build();
  }
}
