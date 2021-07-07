package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.app.datafetcher.delegate.DeleteDelegateDataFetcher;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import io.harness.app.schema.mutation.delegate.QLDeleteDelegateInput;
import io.harness.app.schema.mutation.delegate.QLDeleteDelegatePayload;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeleteDelegateDataFetcherTest extends AbstractDataFetcherTestBase {
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Inject DelegateService delegateService;
  @Inject private DeleteDelegateDataFetcher deleteDelegateDataFetcher;
  @Inject private HPersistence persistence;
  @Inject BroadcasterFactory broadcasterFactory;
  @Mock Broadcaster broadcaster;
  private static final String DELEGATE_TYPE = "dockerType";
  private static final String VERSION = "1.0.0";
  private static final String STREAM_DELEGATE = "/stream/delegate/";
  public static final String APPLICATION_NAME = "APPLICATION_NAME";
  private User user;

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testForceDeleteDelegateDataFetcher() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);
    QLDeleteDelegateInput deleteDelegateInput =
        QLDeleteDelegateInput.builder().delegateId(delegateId).accountId(accountId).forceDelete(true).build();

    QLDeleteDelegatePayload deleteDelegatePayload = deleteDelegateDataFetcher.mutateAndFetch(
        deleteDelegateInput, MutationContext.builder().accountId(accountId).build());

    Assert.assertTrue(deleteDelegatePayload.getMessage().equals("Delegate deleted"));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testNonExistingDeleteDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    QLDeleteDelegateInput deleteDelegateInput =
        QLDeleteDelegateInput.builder().delegateId(delegateId).accountId(accountId).forceDelete(true).build();
    QLDeleteDelegatePayload deleteDelegatePayload = deleteDelegateDataFetcher.mutateAndFetch(
            deleteDelegateInput, MutationContext.builder().accountId(accountId).build());
    Assert.assertTrue(deleteDelegatePayload.getMessage().equals("Unable to complete request to delete delegate"));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testSetStatusDeleteDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);
    QLDeleteDelegateInput deleteDelegateInput =
        QLDeleteDelegateInput.builder().delegateId(delegateId).accountId(accountId).forceDelete(false).build();
    when(broadcasterFactory.lookup(any(), anyBoolean())).thenReturn(broadcaster);
    QLDeleteDelegatePayload deleteDelegatePayload = deleteDelegateDataFetcher.mutateAndFetch(
        deleteDelegateInput, MutationContext.builder().accountId(accountId).build());

    Assert.assertTrue(deleteDelegatePayload.getMessage().equals("Delegate deleted"));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testNonExistingSetStatusDeleteDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    QLDeleteDelegateInput deleteDelegateInput =
            QLDeleteDelegateInput.builder().delegateId(delegateId).accountId(accountId).forceDelete(false).build();
    QLDeleteDelegatePayload deleteDelegatePayload = deleteDelegateDataFetcher.mutateAndFetch(
            deleteDelegateInput, MutationContext.builder().accountId(accountId).build());
    Assert.assertTrue(deleteDelegatePayload.getMessage().equals("Unable to complete request to delete delegate"));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testPermissionDeleteDelegate() {
    user.setUserRequestContext(
            UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    //user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMap(null);
    user.getUserRequestContext().setUserPermissionInfo(null);
    String accountId = generateUuid();
    String delegateId = generateUuid();
    QLDeleteDelegateInput deleteDelegateInput =
            QLDeleteDelegateInput.builder().delegateId(delegateId).accountId(accountId).forceDelete(false).build();
    QLDeleteDelegatePayload deleteDelegatePayload = deleteDelegateDataFetcher.mutateAndFetch(
            deleteDelegateInput, MutationContext.builder().accountId(accountId).build());
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
