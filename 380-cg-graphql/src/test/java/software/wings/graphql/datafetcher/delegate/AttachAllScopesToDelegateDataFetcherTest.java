/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import static org.mockito.Mockito.when;

import io.harness.app.datafetcher.delegate.AttachAllScopesToDelegateDataFetcher;
import io.harness.app.schema.mutation.delegate.input.QLAttachAllScopesToDelegateInput;
import io.harness.app.schema.mutation.delegate.input.QLAttachAllScopesToDelegateInput.QLAttachAllScopesToDelegateInputBuilder;
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
import io.harness.service.intfc.DelegateCache;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Assert;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
public class AttachAllScopesToDelegateDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject private HPersistence persistence;
  @Inject AttachAllScopesToDelegateDataFetcher attachScopeToDelegateDataFetcher;
  @Inject DelegateScopeService delegateScopeService;
  @Inject DelegateService delegateService;
  @Mock private DelegateCache delegateCache;

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
  public void testAttachAllScopesToDelegateWithIncludeScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    String delegateScopeName = "delegateScope22";
    DelegateScope delegateScope = createDelegateScopeBuilder(accountId, delegateScopeName).build();
    persistence.save(delegateScope);

    QLAttachAllScopesToDelegateInputBuilder attachScopeToDelegateInputBuilder =
        QLAttachAllScopesToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId).delegateId(delegateId).build();

    when(delegateCache.get(accountId, delegateId, true)).thenReturn(existingDelegate);
    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(
        qlAttachScopeToDelegatePayload.getMessage().startsWith("Included scopes for delegate:  delegateScope22,"));
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testAttachAllScopesToDelegateWithNoAccountScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder(accountId, delegateId).build();
    persistence.save(existingDelegate);

    QLAttachAllScopesToDelegateInputBuilder attachScopeToDelegateInputBuilder =
        QLAttachAllScopesToDelegateInput.builder();
    attachScopeToDelegateInputBuilder.accountId(accountId).delegateId(delegateId).build();

    QLAttachScopeToDelegatePayload qlAttachScopeToDelegatePayload = attachScopeToDelegateDataFetcher.mutateAndFetch(
        attachScopeToDelegateInputBuilder.build(), MutationContext.builder().build());
    Assert.notNull(qlAttachScopeToDelegatePayload);
    Assert.notNull(qlAttachScopeToDelegatePayload.getMessage());
    Assert.isTrue(
        qlAttachScopeToDelegatePayload.getMessage().startsWith("No Scopes available in account to add to delegate"));
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
