package software.wings.service.impl.notifications;

import static io.harness.rule.OwnerRule.MOUNIK;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.CDC)
public class PagerDutyEventDispatcherTest extends WingsBaseTest {
  @Inject @InjectMocks private PagerDutyEventDispatcher pagerDutyEventDispatcher;
  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void testPagerDuty() {}
}
