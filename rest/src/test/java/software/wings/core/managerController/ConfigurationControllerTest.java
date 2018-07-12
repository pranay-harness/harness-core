package software.wings.core.managerController;

import static io.harness.threading.Morpheus.sleep;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.ManagerConfiguration.Builder.aManagerConfiguration;

import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.ManagerConfiguration;
import software.wings.core.managerConfiguration.ConfigChangeEvent;
import software.wings.core.managerConfiguration.ConfigChangeListener;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.HQuery;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

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

  class TestListener implements ConfigChangeListener {
    private boolean onChangeCalled;
    @Override
    public void onConfigChange(List<ConfigChangeEvent> events) {
      onChangeCalled = true;
    }
  }

  public class BackgroundThread extends Thread {
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
  public void primaryIsNotSet() {
    when(query.get()).thenReturn(aManagerConfiguration().withPrimaryVersion("2.0.0").build());
    sleep(Duration.ofMillis(100));
    assertThat(configurationController.isPrimary()).isFalse();
  }

  @Test
  public void primaryIsSet() {
    when(query.get()).thenReturn(aManagerConfiguration().withPrimaryVersion("1.0.0").build());
    sleep(Duration.ofMillis(100));
    assertThat(configurationController.isPrimary()).isTrue();
  }

  @Test
  @Ignore // ToDo [Puneet] find more reliable way to test this
  public void listenerIsCalled() {
    TestListener testListener = new TestListener();
    configurationController.register(testListener, asList(ConfigChangeEvent.PrimaryChanged));
    when(query.get()).thenReturn(aManagerConfiguration().withPrimaryVersion("1.0.0").build());
    sleep(Duration.ofMillis(100));
    when(query.get()).thenReturn(aManagerConfiguration().withPrimaryVersion("2.0.0").build());
    sleep(Duration.ofMillis(100));
    assertThat(testListener.onChangeCalled).isTrue();
    assertThat(configurationController.isPrimary()).isFalse();
  }
}
