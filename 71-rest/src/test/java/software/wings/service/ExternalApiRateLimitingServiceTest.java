package software.wings.service;

import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.threading.Morpheus.sleep;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.ExternalApiRateLimitingService;

import java.time.Duration;

public class ExternalApiRateLimitingServiceTest extends WingsBaseTest {
  private static final double ERROR_THRESHOLD = 1;
  @Inject private ExternalApiRateLimitingService service;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testAllowedRequests() {
    String key = "abcd";
    double numAllowed = 0;
    long currentTime = System.currentTimeMillis();
    long endTime = currentTime + 3000; // 3 second
    while (System.currentTimeMillis() < endTime) {
      if (!service.rateLimitRequest(key)) {
        numAllowed++;
      }

      // Introducing a sleep to avoid spinning the CPU
      sleep(Duration.ofMillis(20));
    }

    // We are running the test for 3 seconds. So max allowed requests = QPM / (60 / 3) -> QPM / 20
    assertThat(numAllowed).isLessThanOrEqualTo(service.getMaxQPMPerManager() / 20 + ERROR_THRESHOLD);
  }
}
