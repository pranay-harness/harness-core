package software.wings.graphql.datafetcher.user;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import software.wings.graphql.schema.type.QLRequestStatus;
import software.wings.graphql.schema.type.user.QLDeleteUserInput;
import software.wings.graphql.schema.type.user.QLDeleteUserPayload;
import software.wings.service.intfc.UserService;

public class DeleteUserDataFetcherTest extends CategoryTest {
  @Mock UserService userService;

  @InjectMocks @Spy DeleteUserDataFetcher deleteUserDataFetcher = new DeleteUserDataFetcher(userService);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_deleteUser() {
    doNothing().when(userService).delete(anyString(), eq("accountId"));
    final QLDeleteUserInput qlDeleteUserInput = QLDeleteUserInput.builder().id("userId").requestId("request1").build();

    final MutationContext mutationContext = MutationContext.builder()
                                                .accountId("accountId")
                                                .dataFetchingEnvironment(Mockito.mock(DataFetchingEnvironment.class))
                                                .build();

    QLDeleteUserPayload qlDeleteUserPayload = deleteUserDataFetcher.mutateAndFetch(qlDeleteUserInput, mutationContext);
    verify(userService, times(1)).delete(eq("accountId"), eq("userId"));
    Assertions.assertThat(qlDeleteUserPayload.getStatus()).isEqualTo(QLRequestStatus.SUCCESS);
    Assertions.assertThat(qlDeleteUserPayload.getRequestId()).isEqualTo("request1");
  }
}