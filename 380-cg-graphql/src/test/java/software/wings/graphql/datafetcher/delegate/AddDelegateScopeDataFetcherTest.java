package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.app.datafetcher.delegate.AddDelegateScopeDataFetcher;
import io.harness.app.schema.mutation.delegate.input.QLAddDelegateScopeInput;
import io.harness.app.schema.mutation.delegate.payload.QLAddDelegateScopePayload;
import io.harness.app.schema.type.delegate.QLTaskGroup;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Assert;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    QLAddDelegateScopeInput.QLAddDelegateScopeInputBuilder qlAddDelegateScopeInputBuilder =
        QLAddDelegateScopeInput.builder()
            .accountId(accountId)
            .name("DELEGATE_SCOPE")
            .application(null)
            .environment(null)
            .service(null)
            .environmentTypes(Collections.singletonList(QLEnvironmentType.NON_PROD))
            .taskGroups(Collections.singletonList(QLTaskGroup.APPDYNAMICS));
    QLAddDelegateScopePayload addDelegateScopePayload = delegateScopeDataFetcher.mutateAndFetch(
        qlAddDelegateScopeInputBuilder.build(), MutationContext.builder().accountId(accountId).build());
    Assert.notNull(addDelegateScopePayload.getDelegateScope());
  }
  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAddDelegateScope2() {
    String accountId = generateUuid();
    String[] applications = {"app1", "app2"};
    String[] environments = {"env1"};
    String[] services = {"serv1", "sev2"};

    QLIdFilter applicationsFilter = QLIdFilter.builder().values(applications).build();
    QLIdFilter environmentFilter = QLIdFilter.builder().values(environments).build();
    QLIdFilter servicesFilter = QLIdFilter.builder().values(services).build();

    QLAddDelegateScopeInput.QLAddDelegateScopeInputBuilder qlAddDelegateScopeInputBuilder =
        QLAddDelegateScopeInput.builder()
            .accountId(accountId)
            .name("DELEGATE_SCOPE")
            .application(applicationsFilter)
            .environment(environmentFilter)
            .service(servicesFilter)
            .environmentTypes(Collections.singletonList(QLEnvironmentType.NON_PROD))
            .taskGroups(Collections.singletonList(QLTaskGroup.APPDYNAMICS));
    QLAddDelegateScopePayload addDelegateScopePayload = delegateScopeDataFetcher.mutateAndFetch(
        qlAddDelegateScopeInputBuilder.build(), MutationContext.builder().accountId(accountId).build());
    Assert.notNull(addDelegateScopePayload.getDelegateScope());
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAddInvalidDelegateScope() {
    String accountId = generateUuid();
    QLAddDelegateScopeInput.QLAddDelegateScopeInputBuilder qlAddDelegateScopeInputBuilder =
        QLAddDelegateScopeInput.builder().accountId(accountId).name("DELEGATE_SCOPE");
    assertThatThrownBy(()
                           -> delegateScopeDataFetcher.mutateAndFetch(qlAddDelegateScopeInputBuilder.build(),
                               MutationContext.builder().accountId(accountId).build()))
        .isInstanceOf(WingsException.class)
        .hasMessage("INVALID_ARGUMENT");
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testTaskGroupEnumCheck() {
    Set<TaskGroup> taskGroupSet = EnumSet.allOf(TaskGroup.class);
    Set<QLTaskGroup> qlTaskGroupSet = EnumSet.allOf(QLTaskGroup.class);
    Assert.isTrue(taskGroupSet.size() == qlTaskGroupSet.size());
    Map<String, QLTaskGroup> qlTaskGroupTaskMap = new HashMap<>();
    qlTaskGroupSet.forEach(qltaskGroup -> qlTaskGroupTaskMap.put(qltaskGroup.name(), qltaskGroup));
    for (TaskGroup taskGroup : taskGroupSet) {
      Assert.isTrue(qlTaskGroupTaskMap.containsKey(taskGroup.name()));
    }
  }
}
