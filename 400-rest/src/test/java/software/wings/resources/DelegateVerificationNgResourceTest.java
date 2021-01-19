package software.wings.resources;

import static io.harness.rule.OwnerRule.NICOLAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateHeartbeatDetails;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.service.intfc.DelegateService;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class DelegateVerificationNgResourceTest {
  private static final String TEST_ACCOUNT_ID = "testAccountId";
  private static final String TEST_SESSION_ID = "testSessionId";
  private static final String TEST_DELEGATE_ID = "testDelegateId";

  @Mock private DelegateService delegateService;

  private DelegateVerificationNgResource resource;

  @Before
  public void setUp() {
    initMocks(this);
    resource = new DelegateVerificationNgResource(delegateService);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testGetDelegatesHeartbeatDetails_noRegisteredDelegates() {
    when(delegateService.obtainDelegateIds(any(String.class), any(String.class))).thenReturn(null);

    RestResponse<DelegateHeartbeatDetails> delegatesHeartbeatDetails =
        resource.getDelegatesHeartbeatDetails(TEST_ACCOUNT_ID, TEST_SESSION_ID);

    assertThat(delegatesHeartbeatDetails.getResource()).isNotNull();
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfRegisteredDelegates()).isZero();
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfConnectedDelegates()).isZero();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testGetDelegatesHeartbeatDetails_noConnectedDelegates() {
    List<String> registeredDelegateIds = Collections.singletonList(TEST_DELEGATE_ID);

    when(delegateService.obtainDelegateIds(any(String.class), any(String.class))).thenReturn(registeredDelegateIds);
    when(delegateService.getConnectedDelegates(TEST_ACCOUNT_ID, registeredDelegateIds))
        .thenReturn(Collections.emptyList());

    RestResponse<DelegateHeartbeatDetails> delegatesHeartbeatDetails =
        resource.getDelegatesHeartbeatDetails(TEST_ACCOUNT_ID, TEST_SESSION_ID);

    assertThat(delegatesHeartbeatDetails.getResource()).isNotNull();
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfRegisteredDelegates()).isEqualTo(1);
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfConnectedDelegates()).isZero();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testGetDelegatesHeartbeatDetails_connectedDelegates() {
    List<String> registeredDelegateIds = Collections.singletonList(TEST_DELEGATE_ID);

    when(delegateService.obtainDelegateIds(any(String.class), any(String.class))).thenReturn(registeredDelegateIds);
    when(delegateService.getConnectedDelegates(TEST_ACCOUNT_ID, registeredDelegateIds))
        .thenReturn(Collections.singletonList(TEST_DELEGATE_ID));

    RestResponse<DelegateHeartbeatDetails> delegatesHeartbeatDetails =
        resource.getDelegatesHeartbeatDetails(TEST_ACCOUNT_ID, TEST_SESSION_ID);

    assertThat(delegatesHeartbeatDetails.getResource()).isNotNull();
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfRegisteredDelegates()).isEqualTo(1);
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfConnectedDelegates()).isEqualTo(1);
  }
}
