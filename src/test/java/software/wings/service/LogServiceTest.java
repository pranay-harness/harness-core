package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Log;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.LogService;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class LogServiceTest extends WingsBaseTest {
  private static final Log log = aLog()
                                     .withAppId("APP_ID")
                                     .withActivityId("ACTIVITY_ID")
                                     .withHostName("host1")
                                     .withLogLine("INFO 1 2 3")
                                     .withCommandUnitName("CMD_NAME")
                                     .withLogLevel(INFO)
                                     .build();

  @Inject private LogService logService;

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Should list logs.
   */
  @Test
  public void shouldListLogs() {
    wingsPersistence.save(log);
    assertThat(logService.list(new PageRequest<>())).hasSize(1).containsExactly(log);
  }

  /**
   * Should save log.
   */
  @Test
  public void shouldSaveLog() {
    logService.save(log);
    assertThat(wingsPersistence.get(Log.class, log.getAppId(), log.getUuid())).isEqualTo(log);
  }
}
