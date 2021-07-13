package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;
import static io.harness.security.NextGenAuthenticationFilter.X_API_KEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion.$2A;

import io.harness.ApiKeyFilterTestBase;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.rule.Owner;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.token.remote.TokenClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@OwnedBy(PL)
@RunWith(PowerMockRunner.class)
@PrepareForTest({NGRestUtils.class})
public class NextGenAuthenticationFilterTest extends ApiKeyFilterTestBase {
  private static final String accountIdentifier = "accountIdentifier";

  private NextGenAuthenticationFilter authenticationFilter;
  private TokenClient tokenClient;
  private ContainerRequestContext containerRequestContext;
  private String apiKey;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate =
        new Predicate<Pair<ResourceInfo, ContainerRequestContext>>() {
          @Override
          public boolean test(Pair<ResourceInfo, ContainerRequestContext> resourceInfoContainerRequestContextPair) {
            return true;
          }
        };
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put("Bearer", apiKey);
    authenticationFilter =
        Mockito.spy(new NextGenAuthenticationFilter(predicate, null, serviceToSecretMapping, tokenClient));
    tokenClient = Mockito.mock(TokenClient.class);
    containerRequestContext = Mockito.mock(ContainerRequestContext.class);

    final UriInfo mockUriInfo = mock(UriInfo.class);
    doReturn(mockUriInfo).when(containerRequestContext).getUriInfo();
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.put(NGCommonEntityConstants.ACCOUNT_KEY, Lists.newArrayList("accountIdentifier"));
    when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);

    FieldUtils.writeField(authenticationFilter, "tokenClient", tokenClient, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFilter_withoutApiKey() {
    when(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(generateUuid());
    when(authenticationFilter.testRequestPredicate(containerRequestContext)).thenReturn(true);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFilter_sat_valid() {
    String delimiter = ".";
    String uuid = generateUuid();
    String rawPassword = generateUuid();
    String encodedPassword = new BCryptPasswordEncoder($2A, 10).encode(rawPassword);
    apiKey = "sat" + delimiter + uuid + delimiter + rawPassword;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    when(authenticationFilter.testRequestPredicate(containerRequestContext)).thenReturn(true);
    PowerMockito.mockStatic(NGRestUtils.class);
    TokenDTO tokenDTO = TokenDTO.builder()
                            .apiKeyType(ApiKeyType.SERVICE_ACCOUNT)
                            .encodedPassword(encodedPassword)
                            .accountIdentifier(accountIdentifier)
                            .valid(true)
                            .parentIdentifier(generateUuid())
                            .build();
    when(NGRestUtils.getResponse(any())).thenReturn(tokenDTO);
    authenticationFilter.filter(containerRequestContext);
    Principal context = SourcePrincipalContextBuilder.getSourcePrincipal();
    assertThat(context).isNotNull();
    assertThat(context.getType()).isEqualByComparingTo(PrincipalType.SERVICE_ACCOUNT);
    assertThat(context.getName()).isEqualTo(tokenDTO.getParentIdentifier());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFilter_sat_invalid() {
    String delimiter = ".";
    String uuid = generateUuid();
    String rawPassword = generateUuid();
    String encodedPassword = new BCryptPasswordEncoder($2A, 10).encode(rawPassword);
    PowerMockito.mockStatic(NGRestUtils.class);
    TokenDTO tokenDTO = TokenDTO.builder()
                            .apiKeyType(ApiKeyType.SERVICE_ACCOUNT)
                            .encodedPassword(encodedPassword)
                            .valid(true)
                            .accountIdentifier(accountIdentifier)
                            .parentIdentifier(generateUuid())
                            .build();
    when(NGRestUtils.getResponse(any())).thenReturn(tokenDTO);
    when(authenticationFilter.testRequestPredicate(containerRequestContext)).thenReturn(true);

    // Incorrect number of sections in api key
    apiKey = "sat" + delimiter + uuid;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);

    // API key prefix not matching
    apiKey = "pat" + delimiter + uuid + delimiter + rawPassword;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);

    // Token not matching
    apiKey = "sat" + delimiter + uuid + delimiter + rawPassword + "1";
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);

    // Invalid token
    apiKey = "sat" + delimiter + uuid + delimiter + rawPassword;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    tokenDTO.setValid(false);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFilter_pat_valid() {
    String delimiter = ".";
    String uuid = generateUuid();
    String rawPassword = generateUuid();
    String encodedPassword = new BCryptPasswordEncoder($2A, 10).encode(rawPassword);
    apiKey = "pat" + delimiter + uuid + delimiter + rawPassword;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    when(authenticationFilter.testRequestPredicate(containerRequestContext)).thenReturn(true);
    PowerMockito.mockStatic(NGRestUtils.class);
    TokenDTO tokenDTO = TokenDTO.builder()
                            .apiKeyType(ApiKeyType.USER)
                            .encodedPassword(encodedPassword)
                            .valid(true)
                            .accountIdentifier(accountIdentifier)
                            .parentIdentifier(generateUuid())
                            .email("user@harness.io")
                            .username("user")
                            .build();
    when(NGRestUtils.getResponse(any())).thenReturn(tokenDTO);
    authenticationFilter.filter(containerRequestContext);
    Principal context = SourcePrincipalContextBuilder.getSourcePrincipal();
    assertThat(context).isNotNull();
    assertThat(context.getType()).isEqualByComparingTo(PrincipalType.USER);
    assertThat(context.getName()).isEqualTo(tokenDTO.getParentIdentifier());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFilter_pat_invalid() {
    String delimiter = ".";
    String uuid = generateUuid();
    String rawPassword = generateUuid();
    String encodedPassword = new BCryptPasswordEncoder($2A, 10).encode(rawPassword);
    PowerMockito.mockStatic(NGRestUtils.class);
    TokenDTO tokenDTO = TokenDTO.builder()
                            .apiKeyType(ApiKeyType.USER)
                            .encodedPassword(encodedPassword)
                            .valid(true)
                            .accountIdentifier(accountIdentifier)
                            .parentIdentifier(generateUuid())
                            .email("user@harness.io")
                            .username("user")
                            .build();
    when(NGRestUtils.getResponse(any())).thenReturn(tokenDTO);
    when(authenticationFilter.testRequestPredicate(containerRequestContext)).thenReturn(true);

    // Incorrect number of sections in api key
    apiKey = "pat" + delimiter + uuid;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);

    // API key prefix not matching
    apiKey = "sat" + delimiter + uuid + delimiter + rawPassword;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);

    // Token not matching
    apiKey = "pat" + delimiter + uuid + delimiter + rawPassword + "1";
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);

    // Invalid token
    apiKey = "pat" + delimiter + uuid + delimiter + rawPassword;
    when(containerRequestContext.getHeaderString(X_API_KEY)).thenReturn(apiKey);
    tokenDTO.setValid(false);
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext))
        .isInstanceOf(InvalidRequestException.class);
  }
}