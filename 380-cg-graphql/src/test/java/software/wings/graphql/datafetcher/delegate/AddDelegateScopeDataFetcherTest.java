package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.app.datafetcher.delegate.AddDelegateScopeDataFetcher;
import io.harness.app.schema.mutation.delegate.input.QLAddDelegateScopeInput;
import io.harness.app.schema.mutation.delegate.payload.QLAddDelegateScopePayload;
import io.harness.beans.EnvironmentType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Assert;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  public void testAddInvalidDelegateScope() {
    String accountId = generateUuid();
    List<String> applicationList = Arrays.asList("app1", "app2");
    List<String> environmentList = Arrays.asList("env1");
    List<String> serviceList = Arrays.asList("serv1", "sev2");
    List<TaskGroup> taskGroups = Arrays.asList(TaskGroup.JIRA, TaskGroup.AWS);
    List<EnvironmentType> environmentTypeList = Arrays.asList(EnvironmentType.NON_PROD);

    QLAddDelegateScopeInput.QLAddDelegateScopeInputBuilder qlAddDelegateScopeInputBuilder =
        QLAddDelegateScopeInput.builder()
            .accountId(accountId)
            .name("DELEGATE_SCOPE")
            .applications(applicationList)
            .environments(environmentList)
            .services(serviceList);
    QLAddDelegateScopePayload addDelegateScopePayload = delegateScopeDataFetcher.mutateAndFetch(
        qlAddDelegateScopeInputBuilder.build(), MutationContext.builder().accountId(accountId).build());
    Assert.notNull(addDelegateScopePayload.getDelegateScope());
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAddDelegateScope() {
    String accountId = generateUuid();
    QLAddDelegateScopeInput.QLAddDelegateScopeInputBuilder qlAddDelegateScopeInputBuilder =
        QLAddDelegateScopeInput.builder()
            .accountId(accountId)
            .name("DELEGATE_SCOPE")
            .applications(new ArrayList<>())
            .environments(new ArrayList<>());
    assertThatThrownBy(()
                           -> delegateScopeDataFetcher.mutateAndFetch(qlAddDelegateScopeInputBuilder.build(),
                               MutationContext.builder().accountId(accountId).build()))
        .isInstanceOf(WingsException.class)
        .hasMessage("INVALID_ARGUMENT");
  }
}
