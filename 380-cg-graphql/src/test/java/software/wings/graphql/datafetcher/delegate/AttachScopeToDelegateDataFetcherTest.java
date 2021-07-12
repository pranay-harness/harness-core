package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import io.harness.app.datafetcher.delegate.AttachScopeToDelegateDataFetcher;
import io.harness.app.schema.mutation.delegate.input.QLAttachScopeToDelegateInput;
import io.harness.app.schema.mutation.delegate.payload.QLAttachScopeToDelegatePayload;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.TaskGroup;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Assert;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class AttachScopeToDelegateDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject private HPersistence persistence;
  @Inject AttachScopeToDelegateDataFetcher attachScopeToDelegateDataFetcher;
  @Inject DelegateScopeService delegateScopeService;
  @Inject DelegateService delegateService;

  private static final String ACCOUNT_ID = "ACCOUNT-ID";
  private static final String DELEGATE_TYPE = "dockerType";
  private static final String VERSION = "1.0.0";
  private static final String STREAM_DELEGATE = "/stream/delegate/";
  public static final String APPLICATION_NAME = "APPLICATION_NAME";

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAttachScopeToDelegateWithIncludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    String delegateScopeId = "delegateScope22";
    List<String> applicationList = Arrays.asList("app1", "app2");
    List<TaskGroup> taskGroups = Arrays.asList(TaskGroup.JIRA, TaskGroup.AWS);
    DelegateScope delegateScope = DelegateScope.builder()
                                      .accountId(accountId)
                                      .name("DELEGATE_SCOPE_TEST")
                                      .applications(applicationList)
                                      .uuid("delegateScopeId")
                                      .taskTypes(taskGroups)
                                      .build();
    persistence.save(delegateScope);

    List<String> includeScopeList = Arrays.asList(delegateScopeId);
    QLAttachScopeToDelegateInput.QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder =
        QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includeScopeList)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(
        qlAttachScopeToDelegatePayload.getMessage().equals("Scopes updated for delegate delegate id " + delegateId));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAttachScopeToDelegateWithIncludeAndExcludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    String delegateScopeId = "delegateScope22";
    List<String> applicationList = Arrays.asList("app1", "app2");
    List<TaskGroup> taskGroups = Arrays.asList(TaskGroup.JIRA, TaskGroup.AWS);
    DelegateScope delegateScope = DelegateScope.builder()
                                      .accountId(accountId)
                                      .name("DELEGATE_SCOPE_TEST")
                                      .applications(applicationList)
                                      .uuid(delegateScopeId)
                                      .taskTypes(taskGroups)
                                      .build();
    persistence.save(delegateScope);

    String delegateScopeId1 = "delegateScopeId11";
    DelegateScope delegateScope1 = DelegateScope.builder()
                                       .accountId(accountId)
                                       .name("DELEGATE_SCOPE_TEST")
                                       .applications(applicationList)
                                       .uuid(delegateScopeId1)
                                       .taskTypes(taskGroups)
                                       .build();
    persistence.save(delegateScope);

    List<String> includeScopeList = Arrays.asList(delegateScopeId);
    List<String> excludeScopeList = Arrays.asList(delegateScopeId1);
    QLAttachScopeToDelegateInput.QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder =
        QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includeScopeList)
        .excludeScopes(excludeScopeList)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(
        qlAttachScopeToDelegatePayload.getMessage().equals("Scopes updated for delegate delegate id " + delegateId));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAttachScopeToNonExistingDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String delegateScopeId = "delegateScope22";
    List<String> applicationList = Arrays.asList("app1", "app2");
    List<TaskGroup> taskGroups = Arrays.asList(TaskGroup.JIRA, TaskGroup.AWS);
    DelegateScope delegateScope = DelegateScope.builder()
                                      .accountId(accountId)
                                      .name("DELEGATE_SCOPE_TEST")
                                      .applications(applicationList)
                                      .uuid(delegateScopeId)
                                      .taskTypes(taskGroups)
                                      .build();
    persistence.save(delegateScope);
    List<String> includeScopeList = Arrays.asList(delegateScopeId);
    QLAttachScopeToDelegateInput.QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder =
        QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includeScopeList)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(
        qlAttachScopeToDelegatePayload.getMessage().equals("Unable to fetch delegate with delegate id " + delegateId));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testAttachScopeToDelegateWithNoIncludeAndExcludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    QLAttachScopeToDelegateInput.QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder =
        QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId).delegateId(delegateId).build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals("No scopes to attach to delegate"));
  }

  private Delegate.DelegateBuilder createDelegateBuilder() {
    return Delegate.builder()
        .accountId(ACCOUNT_ID)
        .ip("127.0.0.1")
        .hostName("localhost")
        .delegateName("testDelegateName")
        .delegateType(DELEGATE_TYPE)
        .version(VERSION)
        .status(DelegateInstanceStatus.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }
}
