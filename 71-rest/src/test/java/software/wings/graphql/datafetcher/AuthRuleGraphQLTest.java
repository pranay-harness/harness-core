package software.wings.graphql.datafetcher;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.graphql.utils.GraphQLConstants.CREATE_APPLICATION_API_PATH;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.security.AuthRuleFilter;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;

public class AuthRuleGraphQLTest extends CategoryTest {
  @Mock AuthRuleFilter authRuleFilter;
  @Mock AuthHandler authHandler;
  @Mock AuthService authService;
  @Mock HPersistence persistence;
  @Spy
  @InjectMocks
  AuthRuleGraphQL authRuleGraphQL = new AuthRuleGraphQL(authRuleFilter, authHandler, authService, persistence);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_handlePostMutation() {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);
    doReturn(new Field(CREATE_APPLICATION_API_PATH)).when(dataFetchingEnvironment).getField();
    doNothing().when(authService).evictUserPermissionAndRestrictionCacheForAccount("accountid", true, true);
    final MutationContext mutationContext =
        MutationContext.builder().accountId("accountid").dataFetchingEnvironment(dataFetchingEnvironment).build();
    authRuleGraphQL.handlePostMutation(mutationContext, new Object(), new Object());
    verify(authService, times(1)).evictUserPermissionAndRestrictionCacheForAccount("accountid", true, true);
  }
}