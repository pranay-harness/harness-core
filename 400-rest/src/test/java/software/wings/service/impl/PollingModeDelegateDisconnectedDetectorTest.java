package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class PollingModeDelegateDisconnectedDetectorTest extends WingsBaseTest {
  @Inject @InjectMocks private PollingModeDelegateDisconnectedDetector pollingModeDelegateDisconnectedDetector;

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateConnection() {
    DelegateObserver delegateObserver = mock(DelegateObserver.class);
    pollingModeDelegateDisconnectedDetector.getSubject().register(delegateObserver);

    String accountId = generateUuid();

    Delegate delegate1 = Delegate.builder().accountId(accountId).polllingModeEnabled(true).build();
    Delegate delegate2 = Delegate.builder().accountId(accountId).polllingModeEnabled(true).build();
    Delegate delegate3 = Delegate.builder().accountId(accountId).polllingModeEnabled(false).build();

    wingsPersistence.save(delegate1);
    wingsPersistence.save(delegate2);
    wingsPersistence.save(delegate3);

    DelegateConnection delegateConnection1 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate1.getUuid())
                                                 .disconnected(false)
                                                 .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
                                                 .build();

    DelegateConnection delegateConnection2 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate2.getUuid())
                                                 .disconnected(false)
                                                 .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
                                                 .build();

    DelegateConnection delegateConnection2a =
        DelegateConnection.builder()
            .accountId(accountId)
            .delegateId(delegate2.getUuid())
            .disconnected(false)
            .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
            .build();

    DelegateConnection delegateConnection3 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate3.getUuid())
                                                 .disconnected(false)
                                                 .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
                                                 .build();

    wingsPersistence.save(delegateConnection1);
    wingsPersistence.save(delegateConnection2);
    wingsPersistence.save(delegateConnection2a);
    wingsPersistence.save(delegateConnection3);

    pollingModeDelegateDisconnectedDetector.run();

    assertThat(wingsPersistence.get(DelegateConnection.class, delegateConnection1.getUuid()).isDisconnected()).isTrue();
    assertThat(wingsPersistence.get(DelegateConnection.class, delegateConnection2.getUuid()).isDisconnected()).isTrue();
    assertThat(wingsPersistence.get(DelegateConnection.class, delegateConnection2a.getUuid()).isDisconnected())
        .isTrue();
    assertThat(wingsPersistence.get(DelegateConnection.class, delegateConnection3.getUuid()).isDisconnected())
        .isFalse();

    verify(delegateObserver).onDisconnected(accountId, delegate1.getUuid());
  }
}
