package software.wings.delegate.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DelegateLoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {
  private boolean started;

  @Override
  public void start() {
    if (started) {
      return;
    }

    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          this.getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      context.putProperty("buildNo", versionInfoManager.getVersionInfo().getBuildNo());
      context.putProperty("commitId", versionInfoManager.getVersionInfo().getGitCommit());
    } catch (IOException e) {
      // no op
    }

    started = true;
  }

  @Override
  public void stop() {}

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public boolean isResetResistant() {
    return true;
  }

  @Override
  public void onStart(LoggerContext context) {}

  @Override
  public void onReset(LoggerContext context) {}

  @Override
  public void onStop(LoggerContext context) {}

  @Override
  public void onLevelChange(Logger logger, Level level) {}
}