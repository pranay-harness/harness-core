package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;

import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
import org.assertj.core.api.Assertions;
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
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationInput;
import software.wings.graphql.schema.mutation.application.payload.QLUpdateApplicationPayload;
import software.wings.service.intfc.AppService;

import java.util.HashMap;

public class UpdateApplicationDataFetcherTest extends CategoryTest {
  @Mock AppService appService;
  @Mock AuthRuleGraphQL authRuleInstrumentation;
  @Mock DataFetcherUtils utils;
  @InjectMocks
  @Spy
  UpdateApplicationDataFetcher updateApplicationDataFetcher = new UpdateApplicationDataFetcher(appService);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    configureAppService();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_mutateAndFetch() {
    final QLUpdateApplicationInput applicationParameters =
        QLUpdateApplicationInput.builder()
            .clientMutationId("req1")
            .applicationId("appid")
            .name(RequestField.setToNullable("   new app name   "))
            .description(RequestField.setToNullable("new app description"))
            .build();
    final MutationContext mutationContext = MutationContext.builder()
                                                .accountId("accountid")
                                                .dataFetchingEnvironment(Mockito.mock(DataFetchingEnvironment.class))
                                                .build();

    final QLUpdateApplicationPayload qlUpdateApplicationPayload =
        updateApplicationDataFetcher.mutateAndFetch(applicationParameters, mutationContext);
    Assertions.assertThat(qlUpdateApplicationPayload.getClientMutationId()).isEqualTo("req1");

    verify(appService, times(1)).get("appid");
    final ArgumentCaptor<Application> applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
    verify(appService, times(1)).update(applicationArgumentCaptor.capture());

    final Application applicationArgument = applicationArgumentCaptor.getValue();
    Assertions.assertThat(applicationArgument.getAccountId()).isEqualTo("accountid");
    Assertions.assertThat(applicationArgument.getName()).isEqualTo("new app name");
    Assertions.assertThat(applicationArgument.getDescription()).isEqualTo("new app description");
    Assertions.assertThat(applicationArgument.getUuid()).isEqualTo("appid");
    Assertions.assertThat(applicationArgument.getAppId()).isEqualTo("appid");
  }

  private void configureAppService() {
    doReturn(Application.Builder.anApplication()
                 .appId("appid")
                 .accountId("accountid")
                 .uuid("appid")
                 .name("old name")
                 .description("old description")
                 .build())
        .when(appService)
        .get("appid");
    doReturn(Application.Builder.anApplication().build()).when(appService).update(any(Application.class));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_mutateAndFetch_1() {
    final MutationContext mutationContext = MutationContext.builder()
                                                .accountId("accountid")
                                                .dataFetchingEnvironment(Mockito.mock(DataFetchingEnvironment.class))
                                                .build();
    final QLUpdateApplicationPayload qlUpdateApplicationPayload =
        updateApplicationDataFetcher.mutateAndFetch(QLUpdateApplicationInput.builder()
                                                        .applicationId("appid")
                                                        .name(RequestField.setToNull())
                                                        .description(RequestField.notSet())
                                                        .build(),
            mutationContext);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_mutateAndFetch_2() throws Exception {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);
    doReturn(
        ImmutableMap.of("clientMutationId", "req1", "applicationId", "appid", "description", "new app description"))
        .when(dataFetchingEnvironment)
        .getArguments();
    doReturn("accountid").when(utils).getAccountId(dataFetchingEnvironment);

    {
      final QLUpdateApplicationPayload qlUpdateApplicationPayload =
          updateApplicationDataFetcher.get(dataFetchingEnvironment);
      ArgumentCaptor<Application> applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
      verify(appService, times(1)).update(applicationArgumentCaptor.capture());
      final Application applicationArgument = applicationArgumentCaptor.getValue();
      assertThat(applicationArgument.getName()).isEqualTo("old name");
      assertThat(applicationArgument.getDescription()).isEqualTo("new app description");
      assertThat(applicationArgument.getAppId()).isEqualTo("appid");
      assertThat(applicationArgument.getUuid()).isEqualTo("appid");
      assertThat(applicationArgument.getAccountId()).isEqualTo("accountid");
    }
    {
      doReturn(new HashMap<String, String>() {
        {
          put("clientMutationId", "req1");
          put("applicationId", "appid");
          put("name", "new_app_name");
          put("description", null);
        }
      })
          .when(dataFetchingEnvironment)
          .getArguments();
      final QLUpdateApplicationPayload qlUpdateApplicationPayload =
          updateApplicationDataFetcher.get(dataFetchingEnvironment);
      ArgumentCaptor<Application> applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
      verify(appService, times(2)).update(applicationArgumentCaptor.capture());
      final Application applicationArgument = applicationArgumentCaptor.getValue();
      assertThat(applicationArgument.getName()).isEqualTo("new_app_name");
      assertThat(applicationArgument.getDescription()).isNull();
      assertThat(applicationArgument.getAppId()).isEqualTo("appid");
      assertThat(applicationArgument.getUuid()).isEqualTo("appid");
      assertThat(applicationArgument.getAccountId()).isEqualTo("accountid");
    }
  }
}