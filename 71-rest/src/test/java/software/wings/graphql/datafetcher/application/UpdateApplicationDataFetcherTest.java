package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
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
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.QLUpdateApplicationParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplicationInput;
import software.wings.service.intfc.AppService;

public class UpdateApplicationDataFetcherTest extends CategoryTest {
  @Mock AppService appService;

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
    final QLUpdateApplicationParameters applicationParameters =
        QLUpdateApplicationParameters.builder()
            .applicationId("appid")
            .application(QLApplicationInput.builder().name("new app name").description("new app description").build())
            .build();
    final MutationContext mutationContext = MutationContext.builder()
                                                .accountId("accountid")
                                                .dataFetchingEnvironment(Mockito.mock(DataFetchingEnvironment.class))
                                                .build();

    updateApplicationDataFetcher.mutateAndFetch(applicationParameters, mutationContext);

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
    final QLApplication updatedApplication1 =
        updateApplicationDataFetcher.mutateAndFetch(QLUpdateApplicationParameters.builder()
                                                        .applicationId("appid")
                                                        .application(QLApplicationInput.builder().build())
                                                        .build(),
            mutationContext);

    final ArgumentCaptor<Application> applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
  }
}