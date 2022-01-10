/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.datafetcher.delegate.AttachScopeToDelegateDataFetcher;
import io.harness.app.schema.mutation.delegate.input.QLAttachScopeToDelegateInput;
import io.harness.app.schema.mutation.delegate.input.QLAttachScopeToDelegateInput.QLAttachScopeToDelegateInputBuilder;
import io.harness.app.schema.mutation.delegate.payload.QLAttachScopeToDelegatePayload;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateBuilder;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateScope.DelegateScopeBuilder;
import io.harness.delegate.beans.TaskGroup;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
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

@OwnedBy(DEL)
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
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachScopeToDelegateWithIncludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    String delegateScopeName = "delegateScope22";
    DelegateScope delegateScope = createDelegateScopeBuilder(accountId, delegateScopeName).build();
    persistence.save(delegateScope);

    String[] delegateScopeNames = {"delegateScope22"};
    QLIdFilter includedScopeIds = QLIdFilter.builder().operator(QLIdOperator.IN).values(delegateScopeNames).build();
    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includedScopeIds)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals(
        "Included scopes for delegate:  [delegateScope22] Excluded scopes for delegate: []"));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachScopeToDelegateWithExcludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    String delegateScopeName = "delegateScope22";
    DelegateScope delegateScope = createDelegateScopeBuilder(accountId, delegateScopeName).build();
    persistence.save(delegateScope);

    String[] delegateScopeNames = {"delegateScope22"};
    QLIdFilter excludeScopeIds = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(delegateScopeNames).build();
    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .excludeScopes(excludeScopeIds)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals(
        "Included scopes for delegate:  [] Excluded scopes for delegate: [delegateScope22]"));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachScopeToDelegateWithIncludeAndExcludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    String delegateScopeName1 = "delegateScope22";
    DelegateScope delegateScopeForInclude = createDelegateScopeBuilder(accountId, delegateScopeName1).build();
    persistence.save(delegateScopeForInclude);

    String delegateScopeName2 = "delegateScope11";
    DelegateScope delegateScopeForExclude = createDelegateScopeBuilder(accountId, delegateScopeName2).build();
    persistence.save(delegateScopeForExclude);

    String[] includeScopeIds = {"delegateScope22"};
    String[] excludeScopeIds = {"delegateScope11"};
    QLIdFilter includedScopeIds = QLIdFilter.builder().values(includeScopeIds).build();
    QLIdFilter excludedScopeIds = QLIdFilter.builder().values(excludeScopeIds).build();
    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includedScopeIds)
        .excludeScopes(excludedScopeIds)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals(
        "Included scopes for delegate:  [delegateScope22] Excluded scopes for delegate: [delegateScope11]"));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachScopeToNonExistingDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    String delegateScopeName = "delegateScope22";
    DelegateScope delegateScope = createDelegateScopeBuilder(accountId, delegateScopeName).build();
    persistence.save(delegateScope);

    String[] delegateScopeIds = {"delegateScope22"};
    QLIdFilter includedScopeIds = QLIdFilter.builder().values(delegateScopeIds).build();

    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includedScopeIds)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(
        qlAttachScopeToDelegatePayload.getMessage().equals("Unable to fetch delegate with delegate id " + delegateId));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachScopeToDelegateWithMultipleIncludeScopes() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    String delegateScopeName1 = "delegateScope22";
    DelegateScope delegateScope1 = createDelegateScopeBuilder(accountId, delegateScopeName1).build();
    persistence.save(delegateScope1);

    String delegateScopeName2 = "delegateScope11";
    DelegateScope delegateScope2 = createDelegateScopeBuilder(accountId, delegateScopeName2).build();
    persistence.save(delegateScope2);

    String[] includeScopeIds = {"delegateScope22", "delegateScope11"};

    QLIdFilter includedScopeIds = QLIdFilter.builder().operator(QLIdOperator.IN).values(includeScopeIds).build();
    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includedScopeIds)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals(
        "Included scopes for delegate:  [delegateScope22, delegateScope11] Excluded scopes for delegate: []"));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachScopeToDelegateWithNoScopes() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId).delegateId(delegateId).build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals("No scopes to attach to delegate"));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachDuplicatesScopeToDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
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
    existingDelegate.setIncludeScopes(Arrays.asList(delegateScope));
    persistence.save(existingDelegate);

    String[] includeScopeIds = {"delegateScope22"};
    QLIdFilter includedScopeIds = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(includeScopeIds).build();
    QLAttachScopeToDelegateInputBuilder attachScopeToDelegateInputBuilder = QLAttachScopeToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId)
        .delegateId(delegateId)
        .includeScopes(includedScopeIds)
        .build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(qlAttachScopeToDelegatePayload.getMessage().equals(
        "Included scopes for delegate:  [DELEGATE_SCOPE_TEST] Excluded scopes for delegate: []"));
  }

  private DelegateBuilder createDelegateBuilder(String accountId, String delegateId) {
    return Delegate.builder()
        .accountId(accountId)
        .ip("127.0.0.1")
        .uuid(delegateId)
        .hostName("localhost")
        .delegateName("testDelegateName")
        .delegateType(DELEGATE_TYPE)
        .version(VERSION)
        .status(DelegateInstanceStatus.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }

  private DelegateScopeBuilder createDelegateScopeBuilder(String accountId, String delegateScopeName) {
    List<String> applicationList = Arrays.asList("app1", "app2");
    List<TaskGroup> taskGroups = Arrays.asList(TaskGroup.JIRA, TaskGroup.AWS);
    return DelegateScope.builder()
        .accountId(accountId)
        .name(delegateScopeName)
        .applications(applicationList)
        .taskTypes(taskGroups);
  }
}
