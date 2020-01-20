package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;

import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AuthRuleGraphQL;
import software.wings.graphql.datafetcher.BaseDataFetcher;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLCreateApplicationInput;
import software.wings.graphql.schema.mutation.application.payload.QLCreateApplicationPayload;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.service.intfc.AppService;

public class CreateApplicationDataFetcherTest extends CategoryTest {
  @Mock AuthRuleGraphQL authRuleInstrumentation;
  @Mock DataFetcherUtils utils;
  @Mock AppService appService;
  @InjectMocks
  @Spy
  CreateApplicationDataFetcher createApplicationDataFetcher = new CreateApplicationDataFetcher(appService);

  @Before

  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_get() throws Exception {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);
    doReturn(ImmutableMap.of("requestId", "req1", "name", "appname", "description", "app description"))
        .when(dataFetchingEnvironment)
        .getArguments();
    doReturn("accountid1").when(utils).getAccountId(dataFetchingEnvironment);
    final Application savedApplication = Application.Builder.anApplication()
                                             .name("appname")
                                             .description("app description")
                                             .appId("appid")
                                             .accountId("accountid1")
                                             .build();
    doReturn(savedApplication).when(appService).save(any(Application.class));

    final QLCreateApplicationPayload qlCreateApplicationPayload =
        createApplicationDataFetcher.get(dataFetchingEnvironment);
    final QLApplication qlApplication = qlCreateApplicationPayload.getApplication();
    assertThat(qlCreateApplicationPayload.getRequestId()).isEqualTo("req1");
    ArgumentCaptor<Application> applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
    verify(appService, times(1)).save(applicationArgumentCaptor.capture());
    verify(authRuleInstrumentation, times(1))
        .instrumentDataFetcher(
            any(BaseDataFetcher.class), eq(dataFetchingEnvironment), eq(QLCreateApplicationPayload.class));

    verify(authRuleInstrumentation, times(1))
        .handlePostMutation(
            any(MutationContext.class), any(QLCreateApplicationInput.class), any(QLCreateApplicationPayload.class));

    final Application applicationArgument = applicationArgumentCaptor.getValue();
    assertThat(applicationArgument.getName()).isEqualTo("appname");
    assertThat(applicationArgument.getDescription()).isEqualTo("app description");
    assertThat(applicationArgument.getAccountId()).isEqualTo("accountid1");

    assertThat(qlApplication.getId()).isEqualTo(savedApplication.getAppId());
    assertThat(qlApplication.getName()).isEqualTo(savedApplication.getName());
    assertThat(qlApplication.getDescription()).isEqualTo(savedApplication.getDescription());
  }
}