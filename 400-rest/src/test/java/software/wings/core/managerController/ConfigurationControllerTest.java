/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.core.managerController;

import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.beans.ManagerConfiguration.Builder.aManagerConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HQuery;
import io.harness.rule.Owner;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;

import software.wings.WingsBaseTest;
import software.wings.beans.ManagerConfiguration;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

public class ConfigurationControllerTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ExecutorService executorService;
  @Mock private VersionInfoManager versionInfoManager;
  @InjectMocks private ConfigurationController configurationController = new ConfigurationController(1);
  private Thread backgroundThread;
  @Mock private HQuery<ManagerConfiguration> query;

  private static Answer executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }

  public class BackgroundThread extends Thread {
    @Override
    public void run() {
      configurationController.start();
    }
  }

  @Before
  public void setUp() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version("1.0.0").build());

    when(wingsPersistence.createQuery(ManagerConfiguration.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    backgroundThread = new Thread(new BackgroundThread());
    backgroundThread.start();
  }

  @After
  public void Teardown() {
    configurationController.stop();
  }

  @Test
  @Owner(developers = PUNEET, intermittent = true)
  @Category(UnitTests.class)
  public void primaryIsNotSet() {
    when(query.get()).thenReturn(aManagerConfiguration().withPrimaryVersion("2.0.0").build());
    assertEventually(10000, () -> assertThat(configurationController.isPrimary()).isFalse());
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void primaryIsSet() {
    when(query.get()).thenReturn(aManagerConfiguration().withPrimaryVersion("1.0.0").build());
    assertEventually(10000, () -> assertThat(configurationController.isPrimary()).isTrue());
  }

  private void assertEventually(int timeoutInMilliseconds, Runnable assertion) {
    long begin = System.currentTimeMillis();
    long now;
    Throwable lastException = null;
    do {
      now = System.currentTimeMillis();
      try {
        assertion.run();
        return;
      } catch (RuntimeException e) {
        lastException = e;
      } catch (AssertionError e) {
        lastException = e;
      }
      sleep(Duration.ofMillis(10));
    } while ((now - begin) < timeoutInMilliseconds);
    throw new RuntimeException(lastException);
  }
}
