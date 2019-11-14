package software.wings.search.framework.changestreams;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
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
  @Owner(emails = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Intermittent: will fix this test.")
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
