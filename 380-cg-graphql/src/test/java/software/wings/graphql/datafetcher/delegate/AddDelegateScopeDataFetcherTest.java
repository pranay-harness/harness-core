package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import graphql.Assert;
import io.harness.app.datafetcher.delegate.AddDelegateScopeDataFetcher;
import io.harness.app.schema.mutation.delegate.QLAddDelegateScopeInput;
import io.harness.app.schema.mutation.delegate.QLAddDelegateScopePayload;
import io.harness.app.schema.type.delegate.QLDelegateScope;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.QLTaskGroup;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class AddDelegateScopeDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject DelegateScopeService delegateScopeService;
  @Inject AddDelegateScopeDataFetcher delegateScopeDataFetcher;
  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAddDelegateScope() {
    String accountId = generateUuid();
    String delegateScopeId = generateUuid();
    QLDelegateScope qlDelegateScope = QLDelegateScope.builder()
                                          .accountId(accountId)
                                          .name(delegateScopeId)
                                          .applications("APP")
                                          .environment("ENV")
                                          .environmentTypes(QLEnvironmentType.NON_PROD)
                                          .taskGroups(QLTaskGroup.AWS)
                                          .build();
    QLAddDelegateScopeInput delegateScopeInput = QLAddDelegateScopeInput.builder().delegateScope(qlDelegateScope).build();
    QLAddDelegateScopePayload qlAddDelegateScopePayload = delegateScopeDataFetcher.mutateAndFetch(delegateScopeInput, MutationContext.builder().accountId(accountId).build());
    Assert.assertTrue(qlAddDelegateScopePayload.getMessage().equals("Delegate Scope added"));
  }


}
