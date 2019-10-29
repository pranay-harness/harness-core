package software.wings.search.framework.changestreams;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

@Slf4j
public class ChangeTrackerTest extends WingsBaseTest {
  @Inject private ChangeTracker changeTracker;

  @Test
  @Category(UnitTests.class)
  public void changeStreamTrackerTest() {
    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();
    ChangeTrackingInfo<?> changeTrackingInfo =
        new ChangeTrackingInfo<>(Application.class, changeEvent -> logger.info(changeEvent.toString()), null);
    changeTrackingInfos.add(changeTrackingInfo);

    Future f = changeTracker.start(changeTrackingInfos);
    assertThat(f.isDone()).isFalse();

    changeTracker.stop();
  }
}
