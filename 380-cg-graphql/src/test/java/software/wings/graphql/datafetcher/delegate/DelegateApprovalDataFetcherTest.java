package software.wings.graphql.datafetcher.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.JENNY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.app.datafetcher.delegate.DelegateApprovalDataFetcher;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;


import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import io.harness.app.schema.mutation.delegate.QLDelegateApproval;
import io.harness.app.schema.mutation.delegate.QLDelegateApprovalInput;
import io.harness.app.schema.mutation.delegate.QLDelegateApprovalPayload;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Assert;
import java.sql.SQLException;
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
