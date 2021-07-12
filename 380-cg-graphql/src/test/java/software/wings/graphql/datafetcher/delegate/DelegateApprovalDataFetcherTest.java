package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.app.datafetcher.delegate.DelegateApprovalDataFetcher;
import io.harness.app.schema.mutation.delegate.input.QLDelegateApprovalInput;
import io.harness.app.schema.mutation.delegate.payload.QLDelegateApprovalPayload;
import io.harness.app.schema.type.delegate.QLDelegateApproval;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Assert;
import java.sql.SQLException;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DelegateApprovalDataFetcherTest extends AbstractDataFetcherTestBase {
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Inject DelegateService delegateService;
  @Inject private DelegateApprovalDataFetcher delegateApprovalDataFetcher;
  @Inject private HPersistence persistence;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Mock private Broadcaster broadcaster;
  private static final String DELEGATE_TYPE = "dockerType";
  private static final String VERSION = "1.0.0";

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testApproveFromDelegateApprovalDataFetcher() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    QLDelegateApprovalInput input = QLDelegateApprovalInput.builder()
                                        .accountId(existingDelegate.getAccountId())
                                        .delegateId(existingDelegate.getUuid())
                                        .delegateApproval(QLDelegateApproval.ACTIVATE)
                                        .build();
    QLDelegateApprovalPayload delegateApprovalPayload =
        delegateApprovalDataFetcher.mutateAndFetch(input, MutationContext.builder().accountId(accountId).build());
    Assert.notNull(delegateApprovalPayload.getDelegate());
    Assert.isTrue(delegateApprovalPayload.getDelegate().getStatus().equals(DelegateInstanceStatus.ENABLED.toString()));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testRejectFromDelegateApprovalDataFetcher() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    QLDelegateApprovalInput input = QLDelegateApprovalInput.builder()
                                        .accountId(existingDelegate.getAccountId())
                                        .delegateId(existingDelegate.getUuid())
                                        .delegateApproval(QLDelegateApproval.REJECT)
                                        .build();
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
    QLDelegateApprovalPayload delegateApprovalPayload =
        delegateApprovalDataFetcher.mutateAndFetch(input, MutationContext.builder().accountId(accountId).build());
    Assert.notNull(delegateApprovalPayload.getDelegate());
    Assert.isTrue(delegateApprovalPayload.getDelegate().getStatus().equals(DelegateInstanceStatus.DELETED.toString()));
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testApprovalActionOnAlreadyApprovedRejectedDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.ENABLED);
    persistence.save(existingDelegate);
    QLDelegateApprovalInput input = QLDelegateApprovalInput.builder()
                                        .accountId(existingDelegate.getAccountId())
                                        .delegateId(existingDelegate.getUuid())
                                        .delegateApproval(QLDelegateApproval.REJECT)
                                        .build();
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
    assertThatThrownBy(
        () -> delegateApprovalDataFetcher.mutateAndFetch(input, MutationContext.builder().accountId(accountId).build()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Delegate is already in state ENABLED");
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = JENNY)
  public void testApprovalActionOnNonExistingDelegate() {
    String accountId = generateUuid();
    String delegateId = "TEST_DELEGATE_ID";

    QLDelegateApprovalInput input = QLDelegateApprovalInput.builder()
                                        .accountId(accountId)
                                        .delegateId(delegateId)
                                        .delegateApproval(QLDelegateApproval.ACTIVATE)
                                        .build();
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
    assertThatThrownBy(
        () -> delegateApprovalDataFetcher.mutateAndFetch(input, MutationContext.builder().accountId(accountId).build()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unable to fetch delegate with delegate ID TEST_DELEGATE_ID");
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
