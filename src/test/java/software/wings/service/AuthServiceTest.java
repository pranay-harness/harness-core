package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
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
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_NAME;

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
import software.wings.service.intfc.AuthService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 8/31/16.
 */
public class AuthServiceTest extends WingsBaseTest {
  @Mock private GenericDbCache cache;

  @Inject @InjectMocks private AuthService authService;

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

  private Builder userBuilder =
      anUser().withAppId(APP_ID).withEmail(USER_EMAIL).withName(USER_NAME).withPassword(PASSWORD);

  @Before
  public void setUp() throws Exception {
    when(cache.get(AuthToken.class, VALID_TOKEN)).thenReturn(new AuthToken(userBuilder.but().build(), 86400000L));
    when(cache.get(AuthToken.class, EXPIRED_TOKEN)).thenReturn(new AuthToken(userBuilder.but().build(), 0L));
    when(cache.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).build());
    when(cache.get(Environment.class, ENV_ID))
        .thenReturn(
            anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withEnvironmentType(EnvironmentType.OTHER).build());
  }

  @Test
  public void shouldValidateValidToken() {
    AuthToken authToken = authService.validateToken(VALID_TOKEN);
    Assertions.assertThat(authToken).isNotNull().isInstanceOf(AuthToken.class);
  }

  @Test
  public void shouldThrowInvalidTokenExceptionForInvalidToken() {
    assertThatThrownBy(() -> authService.validateToken(INVALID_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.INVALID_TOKEN.name());
  }

  @Test
  public void shouldThrowExpiredTokenExceptionForExpiredToken() {
    assertThatThrownBy(() -> authService.validateToken(EXPIRED_TOKEN))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.EXPIRED_TOKEN.name());
  }

  @Test
  public void shouldAuthorizeAppScopeResourceReadRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceReadActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:READ", APP)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:READ", APP)), PageRequestType.OTHER);
  }

  @Test
  public void shouldDenyAppScopeResourceReadRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(envAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("APPLICATION:READ", APP)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldAuthorizeAppScopeResourceWriteRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceWriteActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:WRITE", APP)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
        asList(new PermissionAttribute("APPLICATION:WRITE", APP)), PageRequestType.OTHER);
  }

  @Test
  public void shouldDenyAppScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(envAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("APPLICATION:WRITE", APP)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldAuthorizeEnvScopeResourceReadRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceReadActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:READ", ENV)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceAllActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:READ", ENV)), PageRequestType.OTHER);
  }

  @Test
  public void shouldDenyEnvScopeResourceReadRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("ENVIRONMENT:READ", ENV)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldAuthorizeEnvScopeResourceWriteRequestForUserWithRequiredPermission() {
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceWriteActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:WRITE", ENV)), PageRequestType.OTHER);
    authService.authorize(APP_ID, ENV_ID, userBuilder.but().withRoles(asList(envAllResourceWriteActionRole)).build(),
        asList(new PermissionAttribute("ENVIRONMENT:WRITE", ENV)), PageRequestType.OTHER);
  }

  @Test
  public void shouldDenyEnvScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    assertThatThrownBy(()
                           -> authService.authorize(APP_ID, ENV_ID,
                               userBuilder.but().withRoles(asList(appAllResourceAllActionRole)).build(),
                               asList(new PermissionAttribute("ENVIRONMENT:WRITE", ENV)), PageRequestType.OTHER))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.ACCESS_DENIED.name());
  }
}
