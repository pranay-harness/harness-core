package io.harness.ngtriggers.helpers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.rule.Owner;

import java.util.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookConfigHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetSourceRepoToEvent() {
    Map<WebhookSourceRepo, List<WebhookEvent>> map = WebhookConfigHelper.getSourceRepoToEvent();
    Set<WebhookEvent> events = new HashSet<>(map.get(WebhookSourceRepo.GITHUB));
    events.addAll(map.get(WebhookSourceRepo.GITLAB));
    events.addAll(map.get(WebhookSourceRepo.BITBUCKET));

    Set<WebhookEvent> allEvents = EnumSet.allOf(WebhookEvent.class);
    Set<WebhookEvent> eventsNotPresent = new HashSet<>();
    for (WebhookEvent event : allEvents) {
      if (!events.contains(event)) {
        eventsNotPresent.add(event);
      }
    }
    assertThat(eventsNotPresent).isEmpty();
  }
}