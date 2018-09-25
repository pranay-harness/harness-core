package software.wings.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.security.AuthenticationFilter.EXTERNAL_FACING_API_HEADER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.ExternalApiRateLimitingService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.util.Arrays;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

public class AuthenticationFilterTest {
  @Mock WingsPersistence wingsPersistence = mock(WingsPersistence.class);
  @Mock MainConfiguration configuration = mock(MainConfiguration.class);
  @Mock UserService userService = mock(UserService.class);
  @Mock AuthService authService = mock(AuthService.class);
  @Mock AuditService auditService = mock(AuditService.class);
  @Mock AuditHelper auditHelper = mock(AuditHelper.class);
  @Mock ApiKeyService apiKeyService = mock(ApiKeyService.class);
  @Mock ExternalApiRateLimitingService rateLimitingService = mock(ExternalApiRateLimitingService.class);

  @InjectMocks AuthenticationFilter authenticationFilter;

  ContainerRequestContext context = mock(ContainerRequestContext.class);

  SecurityContext securityContext = mock(SecurityContext.class);

  @Before
  public void setUp() {
    authenticationFilter = new AuthenticationFilter(authService, wingsPersistence, configuration, userService,
        auditService, auditHelper, apiKeyService, rateLimitingService);
    authenticationFilter = spy(authenticationFilter);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.isSecure()).thenReturn(true);
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(configuration.getPortal()).thenReturn(portalConfig);
  }

  @Test
  public void testAuthenticationFilterTestOptions() throws IOException {
    when(context.getMethod()).thenReturn(HttpMethod.OPTIONS);
    authenticationFilter.filter(context);
    assertThat(context.getSecurityContext().isSecure()).isTrue();

    doReturn(true).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
    doReturn(false).when(authenticationFilter).externalFacingAPI();
    authenticationFilter.filter(context);
    assertThat(context.getSecurityContext().isSecure()).isTrue();
  }

  @Test
  public void testNoAuthorizationToken() throws IOException {
    try {
      doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
      doReturn(false).when(authenticationFilter).externalFacingAPI();
      authenticationFilter.filter(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThatExceptionOfType(WingsException.class);
    }
  }

  @Test
  public void testDelegateRequestAuthentication() throws IOException {
    when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Delegate token");
    doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
    doReturn(false).when(authenticationFilter).externalFacingAPI();
    doReturn(true).when(authenticationFilter).delegateAPI();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    when(context.getUriInfo()).thenReturn(uriInfo);
    authenticationFilter.filter(context);
    assertThat(context.getSecurityContext().isSecure()).isTrue();
  }

  @Test
  public void testLearningEngineRequestAuthentication() throws IOException {
    when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("LearningEngine token");
    doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
    doReturn(false).when(authenticationFilter).externalFacingAPI();
    doReturn(true).when(authenticationFilter).learningEngineServiceAPI();
    doReturn(false).when(authenticationFilter).delegateAPI();
    authenticationFilter.filter(context);
    assertThat(context.getSecurityContext().isSecure()).isTrue();
  }

  @Test
  public void testExternalApiAuthentication() throws IOException {
    String apiKey = "ApiKey";
    when(context.getHeaderString(EXTERNAL_FACING_API_HEADER)).thenReturn(apiKey);
    doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
    doReturn(true).when(authenticationFilter).externalFacingAPI();
    doReturn(false).when(rateLimitingService).rateLimitRequest(anyString());
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.put("accountId", Arrays.asList(ACCOUNT_ID));
    when(uriInfo.getQueryParameters()).thenReturn(queryParams);
    when(uriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
    when(context.getUriInfo()).thenReturn(uriInfo);
    authenticationFilter.filter(context);
    verify(apiKeyService).validate(eq(apiKey), eq(ACCOUNT_ID));
    assertThat(context.getSecurityContext().isSecure()).isTrue();
  }

  @Test
  public void testExternalApiRateLimiting() throws IOException {
    String apiKey = "ApiKey";
    when(context.getHeaderString(EXTERNAL_FACING_API_HEADER)).thenReturn(apiKey);
    doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
    doReturn(true).when(authenticationFilter).externalFacingAPI();
    doReturn(true).when(rateLimitingService).rateLimitRequest(anyString());
    assertThatThrownBy(() -> authenticationFilter.filter(context)).isInstanceOf(WebApplicationException.class);
  }

  @Test
  public void testInvalidBearerTokenPresent() throws IOException {
    try {
      when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bearerToken");
      doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
      doReturn(false).when(authenticationFilter).delegateAPI();
      doReturn(false).when(authenticationFilter).externalFacingAPI();
      doReturn(false).when(authenticationFilter).learningEngineServiceAPI();
      when(authService.validateToken(anyString())).thenThrow(new WingsException(ErrorCode.USER_DOES_NOT_EXIST));
      authenticationFilter.filter(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThatExceptionOfType(WingsException.class);
    }
  }

  @Test
  public void testValidBearerTokenPresent() throws IOException {
    try {
      when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bearerToken");
      doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
      doReturn(false).when(authenticationFilter).delegateAPI();
      doReturn(false).when(authenticationFilter).learningEngineServiceAPI();
      doReturn(false).when(authenticationFilter).externalFacingAPI();
      AuthToken authToken = new AuthToken("testUser", 0L);
      authToken.setUser(mock(User.class));
      when(authService.validateToken(anyString())).thenReturn(authToken);
      authenticationFilter.filter(context);
    } catch (WingsException e) {
      fail(e.getMessage(), WingsException.class);
    }
  }

  @Test
  public void testIncorrectToken() throws IOException {
    try {
      when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("fakeToken");
      doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
      doReturn(false).when(authenticationFilter).delegateAPI();
      doReturn(false).when(authenticationFilter).learningEngineServiceAPI();
      doReturn(false).when(authenticationFilter).externalFacingAPI();
      authenticationFilter.filter(context);
    } catch (WingsException e) {
      assertThatExceptionOfType(WingsException.class);
    }
  }
}
