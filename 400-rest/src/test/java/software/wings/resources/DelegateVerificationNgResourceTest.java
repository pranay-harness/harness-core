package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NICOLAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateHeartbeatDetails;
import io.harness.delegate.beans.DelegateInitializationDetails;
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
  private static final String TEST_ORG_ID = generateUuid();
  private static final String TEST_PROJECT_ID = generateUuid();
  private static final String TEST_SESSION_ID = "testSessionId";
  private static final String TEST_DELEGATE_ID = "testDelegateId";
  private static final long TEST_PROFILE_EXECUTION_TIME = System.currentTimeMillis();

  @Mock private DelegateService delegateService;
  @Mock private AccessControlClient accessControlClient;

  private DelegateVerificationNgResource resource;

  @Before
  public void setUp() {
    initMocks(this);
    resource = new DelegateVerificationNgResource(delegateService, accessControlClient);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testGetDelegatesHeartbeatDetails_noRegisteredDelegates() {
    when(delegateService.obtainDelegateIds(any(String.class), any(String.class))).thenReturn(null);

    RestResponse<DelegateHeartbeatDetails> delegatesHeartbeatDetails =
        resource.getDelegatesHeartbeatDetails(TEST_ACCOUNT_ID, TEST_ORG_ID, TEST_PROJECT_ID, TEST_SESSION_ID);

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
        resource.getDelegatesHeartbeatDetails(TEST_ACCOUNT_ID, TEST_ORG_ID, TEST_PROJECT_ID, TEST_SESSION_ID);

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
        resource.getDelegatesHeartbeatDetails(TEST_ACCOUNT_ID, TEST_ORG_ID, TEST_PROJECT_ID, TEST_SESSION_ID);

    assertThat(delegatesHeartbeatDetails.getResource()).isNotNull();
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfRegisteredDelegates()).isEqualTo(1);
    assertThat(delegatesHeartbeatDetails.getResource().getNumberOfConnectedDelegates()).isEqualTo(1);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void getDelegatesInitializationDetails_noDelegateIds() {
    when(delegateService.obtainDelegateIds(any(String.class), any(String.class))).thenReturn(null);

    RestResponse<List<DelegateInitializationDetails>> delegatesInitializationDetails =
        resource.getDelegatesInitializationDetails(TEST_ACCOUNT_ID, TEST_ORG_ID, TEST_PROJECT_ID, TEST_SESSION_ID);

    assertThat(delegatesInitializationDetails.getResource()).isEmpty();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void getDelegatesInitializationDetails_success() {
    List<String> registeredDelegateIds = Collections.singletonList(TEST_DELEGATE_ID);

    when(delegateService.obtainDelegateIds(any(String.class), any(String.class))).thenReturn(registeredDelegateIds);
    when(delegateService.obtainDelegateInitializationDetails(TEST_ACCOUNT_ID, registeredDelegateIds))
        .thenReturn(Collections.singletonList(DelegateInitializationDetails.builder()
                                                  .delegateId(TEST_DELEGATE_ID)
                                                  .initialized(true)
                                                  .profileError(false)
                                                  .profileExecutedAt(TEST_PROFILE_EXECUTION_TIME)
                                                  .build()));

    RestResponse<List<DelegateInitializationDetails>> delegatesInitializationDetails =
        resource.getDelegatesInitializationDetails(TEST_ACCOUNT_ID, TEST_ORG_ID, TEST_PROJECT_ID, TEST_SESSION_ID);
    assertThat(delegatesInitializationDetails).isNotNull();

    List<DelegateInitializationDetails> initializationDetails = delegatesInitializationDetails.getResource();
    assertThat(initializationDetails).isNotEmpty();
    assertThat(initializationDetails.size()).isEqualTo(1);
    assertThat(initializationDetails.get(0).getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(initializationDetails.get(0).isInitialized()).isTrue();
    assertThat(initializationDetails.get(0).isProfileError()).isFalse();
    assertThat(initializationDetails.get(0).getProfileExecutedAt()).isEqualTo(TEST_PROFILE_EXECUTION_TIME);
  }
}
