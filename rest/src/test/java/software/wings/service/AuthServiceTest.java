package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Permission.Builder.aPermission;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.security.PermissionAttribute.Action.ALL;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.WRITE;
import static software.wings.security.PermissionAttribute.PermissionScope.APP;
import static software.wings.security.PermissionAttribute.PermissionScope.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.ANY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_NAME;

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
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Role;
import software.wings.beans.User.Builder;
import software.wings.dl.GenericDbCache;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/31/16.
 */
public class AuthServiceTest extends WingsBaseTest {
  private final String VALID_TOKEN = "VALID_TOKEN";
  private final String INVALID_TOKEN = "INVALID_TOKEN";
  private final String EXPIRED_TOKEN = "EXPIRED_TOKEN";
  private final Role appAllResourceReadActionRole = aRole()
                                                        .withAppId(GLOBAL_APP_ID)
                                                        .withName(ROLE_NAME)
                                                        .withUuid(ROLE_ID)
                                                        .withPermissions(asList(aPermission()
                                                                                    .withAppId(APP_ID)
                                                                                    .withEnvId(ENV_ID)
                                                                                    .withPermissionScope(APP)
                                                                                    .withResourceType(ANY)
                                                                                    .withAction(READ)
                                                                                    .build()))
                                                        .build();
  private final Role appAllResourceWriteActionRole = aRole()
                                                         .withAppId(GLOBAL_APP_ID)
                                                         .withName(ROLE_NAME)
                                                         .withUuid(ROLE_ID)
                                                         .withPermissions(asList(aPermission()
                                                                                     .withAppId(APP_ID)
                                                                                     .withEnvId(ENV_ID)
                                                                                     .withPermissionScope(APP)
                                                                                     .withResourceType(ANY)
                                                                                     .withAction(WRITE)
                                                                                     .build()))
                                                         .build();
  private final Role appAllResourceAllActionRole = aRole()
                                                       .withAppId(GLOBAL_APP_ID)
                                                       .withName(ROLE_NAME)
                                                       .withUuid(ROLE_ID)
                                                       .withPermissions(asList(aPermission()
                                                                                   .withAppId(APP_ID)
                                                                                   .withEnvId(ENV_ID)
                                                                                   .withPermissionScope(APP)
                                                                                   .withResourceType(ANY)
                                                                                   .withAction(ALL)
                                                                                   .build()))
                                                       .build();
  private final Role envAllResourceReadActionRole = aRole()
                                                        .withAppId(GLOBAL_APP_ID)
                                                        .withName(ROLE_NAME)
                                                        .withUuid(ROLE_ID)
                                                        .withPermissions(asList(aPermission()
                                                                                    .withAppId(GLOBAL_APP_ID)
                                                                                    .withEnvId(ENV_ID)
                                                                                    .withPermissionScope(ENV)
                                                                                    .withResourceType(ANY)
                                                                                    .withAction(READ)
                                                                                    .build()))
                                                        .build();
  private final Role envAllResourceWriteActionRole = aRole()
                                                         .withAppId(GLOBAL_APP_ID)
                                                         .withName(ROLE_NAME)
                                                         .withUuid(ROLE_ID)
                                                         .withPermissions(asList(aPermission()
                                                                                     .withAppId(GLOBAL_APP_ID)
                                                                                     .withEnvId(ENV_ID)
                                                                                     .withPermissionScope(ENV)
                                                                                     .withResourceType(ANY)
                                                                                     .withAction(WRITE)
                                                                                     .build()))
                                                         .build();
  private final Role envAllResourceAllActionRole = aRole()
                                                       .withAppId(GLOBAL_APP_ID)
                                                       .withName(ROLE_NAME)
                                                       .withUuid(ROLE_ID)
                                                       .withPermissions(asList(aPermission()
                                                                                   .withAppId(GLOBAL_APP_ID)
                                                                                   .withEnvId(ENV_ID)
                                                                                   .withPermissionScope(ENV)
                                                                                   .withResourceType(ANY)
                                                                                   .withAction(ALL)
                                                                                   .build()))
                                                       .build();
  @Mock private GenericDbCache cache;
  @Mock private AccountService accountService;

  @Inject @InjectMocks private AuthService authService;
  private Builder userBuilder =
      anUser().withAppId(APP_ID).withEmail(USER_EMAIL).withName(USER_NAME).withPassword(PASSWORD);
  private String accountKey = "2f6b0988b6fb3370073c3d0505baee59";
  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(cache.get(AuthToken.class, VALID_TOKEN)).thenReturn(new AuthToken(userBuilder.but().build(), 86400000L));
    when(cache.get(AuthToken.class, EXPIRED_TOKEN)).thenReturn(new AuthToken(userBuilder.but().build(), 0L));
    when(cache.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).build());
    when(cache.get(Environment.class, ENV_ID))
        .thenReturn(
            anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withEnvironmentType(EnvironmentType.OTHER).build());
    when(accountService.get(ACCOUNT_ID))
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
        .hasMessage(ErrorCodes.INVALID_TOKEN.name());
  }

  /**
   * Should throw expired token exception for expired token.
   */
  @Test
  public void shouldThrowExpiredTokenExceptionForExpiredToken() {
    assertThatThrownBy(() -> authService.validateToken(EXPIRED_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.EXPIRED_TOKEN.name());
  }

  /**
   * Should authorize app scope resource read request for user with required permission.
   */
  @Test
  public void shouldAuthorizeAppScopeResourceReadRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceReadActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:READ", APP)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:READ", APP)), PageRequestType.OTHER);
  }

  /**
   * Should deny app scope resource read request for user without required permission.
   */
  @Test
  public void shouldDenyAppScopeResourceReadRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(envAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("APPLICATION:READ", APP)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  /**
   * Should authorize app scope resource write request for user with required permission.
   */
  @Test
  public void shouldAuthorizeAppScopeResourceWriteRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceWriteActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:WRITE", APP)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:WRITE", APP)), PageRequestType.OTHER);
  }

  /**
   * Should deny app scope resource write request for user without required permission.
   */
  @Test
  public void shouldDenyAppScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(envAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("APPLICATION:WRITE", APP)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  /**
   * Should authorize env scope resource read request for user with required permission.
   */
  @Test
  public void shouldAuthorizeEnvScopeResourceReadRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceReadActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:READ", ENV)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceAllActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:READ", ENV)), PageRequestType.OTHER);
  }

  /**
   * Should deny env scope resource read request for user without required permission.
   */
  @Test
  public void shouldDenyEnvScopeResourceReadRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("ENVIRONMENT:READ", ENV)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  /**
   * Should authorize env scope resource write request for user with required permission.
   */
  @Test
  public void shouldAuthorizeEnvScopeResourceWriteRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceWriteActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:WRITE", ENV)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceWriteActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:WRITE", ENV)), PageRequestType.OTHER);
  }

  /**
   * Should deny env scope resource write request for user without required permission.
   */
  @Test
  public void shouldDenyEnvScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("ENVIRONMENT:WRITE", ENV)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldValidateDelegateToken() {
    authService.validateDelegateToken(ACCOUNT_ID, getDelegateToken(accountKey));
  }

  @Test
  public void shouldThrowDenyAccessWhenAccountIdNotFoundForDelegate() {
    assertThatThrownBy(() -> authService.validateDelegateToken(ACCOUNT_ID + "1", getDelegateToken(accountKey)))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldThrowThrowInavlidTokenForDelegate() {
    assertThatThrownBy(() -> authService.validateDelegateToken(ACCOUNT_ID, "Dummy"))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.INVALID_TOKEN.name());
  }

  @Test
  public void shouldThrowExceptionWhenUnableToDecryptToken() {
    assertThatThrownBy(() -> authService.validateDelegateToken(ACCOUNT_ID, getDelegateToken()))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.INVALID_TOKEN.name());
  }

  private String getDelegateToken() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      throw new WingsException(ErrorCodes.DEFAULT_ERROR_CODE);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    return getDelegateToken(Hex.encodeHexString(encoded));
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
