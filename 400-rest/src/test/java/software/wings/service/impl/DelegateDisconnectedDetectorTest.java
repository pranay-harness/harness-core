/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.version.VersionInfoManager;

import software.wings.WingsBaseTest;
import software.wings.beans.DelegateConnection;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateDisconnectedDetectorTest extends WingsBaseTest {
  @Inject HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject @Spy private DelegateDisconnectedDetector delegateDisconnectedDetector;

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateConnection() {
    String accountId = generateUuid();

    Delegate delegate1 = Delegate.builder().accountId(accountId).polllingModeEnabled(true).build();
    Delegate delegate2 = Delegate.builder().accountId(accountId).polllingModeEnabled(false).build();

    persistence.save(delegate1);
    persistence.save(delegate2);

    DelegateConnection delegateConnection1 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate1.getUuid())
                                                 .disconnected(false)
                                                 .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
                                                 .version(versionInfoManager.getVersionInfo().getVersion())
                                                 .build();

    DelegateConnection delegateConnection1a =
        DelegateConnection.builder()
            .accountId(accountId)
            .delegateId(delegate1.getUuid())
            .disconnected(false)
            .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
            .version("dummy")
            .build();

    DelegateConnection delegateConnection1b = DelegateConnection.builder()
                                                  .accountId(accountId)
                                                  .delegateId(delegate1.getUuid())
                                                  .disconnected(false)
                                                  .lastHeartbeat(currentTimeMillis())
                                                  .version(versionInfoManager.getVersionInfo().getVersion())
                                                  .build();

    DelegateConnection delegateConnection2 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate2.getUuid())
                                                 .disconnected(false)
                                                 .lastHeartbeat(currentTimeMillis() - Duration.ofMinutes(10).toMillis())
                                                 .version("dummy")
                                                 .build();

    persistence.save(delegateConnection1);
    persistence.save(delegateConnection1a);
    persistence.save(delegateConnection1b);
    persistence.save(delegateConnection2);

    doCallRealMethod().when(delegateDisconnectedDetector).run();

    delegateDisconnectedDetector.run();

    assertThat(persistence.get(DelegateConnection.class, delegateConnection1.getUuid()).isDisconnected()).isTrue();
    assertThat(persistence.get(DelegateConnection.class, delegateConnection1a.getUuid())).isNull();
    assertThat(persistence.get(DelegateConnection.class, delegateConnection1b.getUuid()).isDisconnected()).isFalse();
    assertThat(persistence.get(DelegateConnection.class, delegateConnection2.getUuid())).isNull();

    verify(delegateDisconnectedDetector).disconnectedDetected(true, delegateConnection1);
    verify(delegateDisconnectedDetector).disconnectedDetected(false, delegateConnection2);
  }
}
