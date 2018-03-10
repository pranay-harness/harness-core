package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.utils.WingsIntegrationTestConstants.delegateAccountSecret;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.dl.GenericDbCache;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.TokenGenerator;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.utils.CacheHelper;

import java.security.NoSuchAlgorithmException;
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

  @Mock private GenericDbCache cache;
  @Mock private static CacheHelper cacheHelper;
  @Mock private Cache<String, User> userCache;

  @Mock private AccountService accountService;

  @Inject @InjectMocks private AuthService authService;
  private Builder userBuilder =
      anUser().withAppId(APP_ID).withEmail(USER_EMAIL).withName(USER_NAME).withPassword(PASSWORD);
  private String accountKey = delegateAccountSecret;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(cacheHelper.getUserCache()).thenReturn(userCache);
    when(userCache.get(USER_ID)).thenReturn(User.Builder.anUser().withUuid(USER_ID).build());

    when(cache.get(AuthToken.class, VALID_TOKEN)).thenReturn(new AuthToken(USER_ID, 86400000L));
    when(cache.get(AuthToken.class, EXPIRED_TOKEN)).thenReturn(new AuthToken(USER_ID, 0L));
    when(cache.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).build());
    when(cache.get(Environment.class, ENV_ID))
        .thenReturn(
            anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withEnvironmentType(EnvironmentType.NON_PROD).build());
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withUuid(ACCOUNT_ID).withAccountKey(accountKey).build());
    when(cache.get(Account.class, ACCOUNT_ID))
        .thenReturn(anAccount().withUuid(ACCOUNT_ID).withAccountKey(accountKey).build());
  }

  /**
   * Should validate valid token.
   */
  @Test
  public void shouldValidateValidToken() {
    AuthToken authToken = authService.validateToken(VALID_TOKEN);
    Assertions.assertThat(authToken).isNotNull().isInstanceOf(AuthToken.class);
  }

  /**
   * Should throw invalid token exception for invalid token.
   */
  @Test
  public void shouldThrowInvalidTokenExceptionForInvalidToken() {
    assertThatThrownBy(() -> authService.validateToken(INVALID_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_TOKEN.name());
  }

  /**
   * Should throw expired token exception for expired token.
   */
  @Test
  public void shouldThrowExpiredTokenExceptionForExpiredToken() {
    assertThatThrownBy(() -> authService.validateToken(EXPIRED_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.EXPIRED_TOKEN.name());
  }

  @Test
  public void shouldAuthorizeWithAccountAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.ACCOUNT_ADMIN).build();
    User user = userBuilder.but().withRoles(asList(role)).build();
    String appId = null;
    authService.authorize(
        ACCOUNT_ID, appId, null, user, asList(new PermissionAttribute(ResourceType.USER, Action.READ)), null);
  }

  @Test
  public void shouldDenyWithoutAccountAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.APPLICATION_ADMIN).build();
    role.onLoad();
    User user = userBuilder.but().withRoles(asList(role)).build();
    String appId = null;
    assertThatThrownBy(()
                           -> authService.authorize(ACCOUNT_ID, appId, null, user,
                               asList(new PermissionAttribute(ResourceType.USER, Action.READ)), null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  public void shouldAuthorizeWithAppAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.APPLICATION_ADMIN).withAppId(APP_ID).build();
    role.onLoad();
    User user = userBuilder.but().withRoles(asList(role)).build();
    authService.authorize(
        ACCOUNT_ID, APP_ID, null, user, asList(new PermissionAttribute(ResourceType.ARTIFACT, Action.UPDATE)), null);
  }

  @Test
  public void shouldAuthorizeReadWithEnvAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.NON_PROD_SUPPORT).withAppId(APP_ID).build();
    role.onLoad();
    User user = userBuilder.but().withRoles(asList(role)).build();
    authService.authorize(
        ACCOUNT_ID, APP_ID, ENV_ID, user, asList(new PermissionAttribute(ResourceType.APPLICATION, Action.READ)), null);
  }

  @Test
  public void shouldDenyWithDiffAppAdminAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.APPLICATION_ADMIN).withAppId("APP_ID2").build();
    role.onLoad();
    User user = userBuilder.but().withRoles(asList(role)).build();
    assertThatThrownBy(()
                           -> authService.authorize(ACCOUNT_ID, APP_ID, null, user,
                               asList(new PermissionAttribute(ResourceType.APPLICATION, Action.READ)), null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  public void shouldDenyWriteWithEnvAccess() {
    Role role = aRole().withAccountId(ACCOUNT_ID).withRoleType(RoleType.NON_PROD_SUPPORT).withAppId(APP_ID).build();
    role.onLoad();
    User user = userBuilder.but().withRoles(asList(role)).build();
    assertThatThrownBy(()
                           -> authService.authorize(ACCOUNT_ID, APP_ID, ENV_ID, user,
                               asList(new PermissionAttribute(ResourceType.APPLICATION, Action.UPDATE)), null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  public void shouldValidateDelegateToken() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    authService.validateDelegateToken(ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname"));
  }

  @Test
  public void shouldThrowDenyAccessWhenAccountIdNotFoundForDelegate() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    assertThatThrownBy(()
                           -> authService.validateDelegateToken(
                               ACCOUNT_ID + "1", tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.ACCESS_DENIED.name());
  }

  @Test
  public void shouldThrowThrowInavlidTokenForDelegate() {
    assertThatThrownBy(() -> authService.validateDelegateToken(ACCOUNT_ID, "Dummy"))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_TOKEN.name());
  }

  @Test
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
}
