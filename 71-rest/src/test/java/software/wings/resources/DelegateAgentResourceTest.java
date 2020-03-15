package software.wings.resources;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import io.harness.artifact.ArtifactCollectionResponseHandler;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.ratelimit.DelegateRequestRateLimiter;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.ResourceTestRule;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

@RunWith(Parameterized.class)
@Slf4j
public class DelegateAgentResourceTest {
  private static DelegateService delegateService = mock(DelegateService.class);

  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

  private static AccountService accountService = mock(AccountService.class);
  private static WingsPersistence wingsPersistence = mock(WingsPersistence.class);
  private static DelegateRequestRateLimiter delegateRequestRateLimiter = mock(DelegateRequestRateLimiter.class);
  private static SubdomainUrlHelperIntfc subdomainUrlHelper = mock(SubdomainUrlHelperIntfc.class);
  private static ArtifactCollectionResponseHandler artifactCollectionResponseHandler =
      mock(ArtifactCollectionResponseHandler.class);

  @Parameter public String apiUrl;

  @Parameters
  public static String[] data() {
    return new String[] {null, "https://testUrl"};
  }

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .addResource(new DelegateAgentResource(delegateService, accountService, wingsPersistence,
              delegateRequestRateLimiter, subdomainUrlHelper, artifactCollectionResponseHandler))
          .addResource(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .addProvider(WingsExceptionMapper.class)
          .build();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetDelegateConfiguration() {
    DelegateConfiguration delegateConfiguration = DelegateConfiguration.builder().build();
    when(accountService.getDelegateConfiguration(ACCOUNT_ID)).thenReturn(delegateConfiguration);
    RestResponse<DelegateConfiguration> restResponse =
        RESOURCES.client()
            .target("/agent/delegates/configuration?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<DelegateConfiguration>>() {});

    verify(accountService, atLeastOnce()).getDelegateConfiguration(ACCOUNT_ID);
    assertThat(restResponse.getResource()).isInstanceOf(DelegateConfiguration.class).isNotNull();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetconnectionHeartbeat() {
    DelegateConnectionHeartbeat delegateConnectionHeartbeat = DelegateConnectionHeartbeat.builder().build();
    RestResponse<String> restResponse = RESOURCES.client()
                                            .target("/agent/delegates/connectionHeartbeat/" + DELEGATE_ID
                                                + "?delegateId=" + DELEGATE_ID + "&accountId=" + ACCOUNT_ID)
                                            .request()
                                            .post(entity(delegateConnectionHeartbeat, MediaType.APPLICATION_JSON),
                                                new GenericType<RestResponse<String>>() {});
    verify(delegateService, atLeastOnce()).doConnectionHeartbeat(ACCOUNT_ID, DELEGATE_ID, delegateConnectionHeartbeat);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldClearCache() {
    Delegate delegate = Delegate.builder().build();
    when(delegateService.update(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));

    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/agent/delegates/" + DELEGATE_ID + "/clear-cache?delegateId=" + DELEGATE_ID
                + "&accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});
    verify(delegateService, atLeastOnce()).clearCache(ACCOUNT_ID, DELEGATE_ID);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldRegisterDelegate() {
    when(delegateService.register(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/agent/delegates/register?accountId=" + ACCOUNT_ID)
            .request()
            .post(entity(Delegate.builder().uuid(ID_KEY).build(), MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<Delegate>>() {});

    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(delegateService, atLeastOnce()).register(captor.capture());
    Delegate captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(captorValue.getUuid()).isEqualTo(ID_KEY);
    Delegate resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(resource.getUuid()).isEqualTo(ID_KEY);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldAddDelegate() {
    Delegate delegate = Delegate.builder().build();

    when(delegateService.add(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/agent/delegates?accountId=" + ACCOUNT_ID)
            .request()
            .post(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(delegateService, atLeastOnce()).add(captor.capture());
    Delegate captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    Delegate resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldProcessArtifactCollection() {
    BuildSourceExecutionResponse buildSourceExecutionResponse =
        BuildSourceExecutionResponse.builder()
            .commandExecutionStatus(SUCCESS)
            .artifactStreamId(ARTIFACT_STREAM_ID)
            .buildSourceResponse(BuildSourceResponse.builder().build())
            .build();

    RestResponse<Boolean> restResponse =
        RESOURCES.client()
            .target("/agent/delegates/artifact-collection/12345679?accountId=" + ACCOUNT_ID)
            .request()
            .post(entity(buildSourceExecutionResponse, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<Boolean>>() {});

    verify(artifactCollectionResponseHandler, atLeastOnce())
        .processArtifactCollectionResult(buildSourceExecutionResponse);
  }
}
