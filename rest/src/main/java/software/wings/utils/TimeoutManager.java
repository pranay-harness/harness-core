package software.wings.utils;

import static com.ifesdjeen.timer.HashedWheelTimer.DEFAULT_RESOLUTION;
import static com.ifesdjeen.timer.HashedWheelTimer.DEFAULT_WHEEL_SIZE;

import com.ifesdjeen.timer.HashedWheelTimer;
import com.ifesdjeen.timer.WaitStrategy;
import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by peeyushaggarwal on 7/27/16.
 */
public class TimeoutManager implements Managed {
  private ScheduledExecutorService scheduledExecutorService =
      new HashedWheelTimer(DEFAULT_RESOLUTION, DEFAULT_WHEEL_SIZE, new WaitStrategy.YieldingWait());

  @Override
  public void start() throws Exception {}

  @Override
  public void stop() throws Exception {}
}
