package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationInput;
import software.wings.graphql.schema.mutation.application.payload.QLDeleteApplicationPayload;
import software.wings.service.intfc.AppService;

public class DeleteApplicationDataFetcherTest extends CategoryTest {
  @Mock AppService appService;

  @InjectMocks
  @Spy
  DeleteApplicationDataFetcher deleteApplicationDataFetcher = new DeleteApplicationDataFetcher(appService);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void mutateAndFetch() {
    doNothing().when(appService).delete(anyString());
    final QLUpdateApplicationInput applicationParameters =

        QLUpdateApplicationInput.builder().applicationId("appid").requestId("req1").build();
    final MutationContext mutationContext = MutationContext.builder()
                                                .accountId("accountid")
                                                .dataFetchingEnvironment(Mockito.mock(DataFetchingEnvironment.class))
                                                .build();

    final QLDeleteApplicationPayload qlDeleteApplicationPayload =
        deleteApplicationDataFetcher.mutateAndFetch(applicationParameters, mutationContext);
    verify(appService, times(1)).delete("appid");
    Assertions.assertThat(qlDeleteApplicationPayload.getSuccess()).isTrue();
    Assertions.assertThat(qlDeleteApplicationPayload.getRequestId()).isEqualTo("req1");
  }
}