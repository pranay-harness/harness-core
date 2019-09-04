package software.wings.service.impl;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventsCollectionConfigExtractorTest {
  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractPublishTargetGivenUrlWithPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io/ccm";
    assertThat(EventsCollectionConfigExtractor.extractPublishTarget(managerUrl)).isEqualTo("pr.harness.io");
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractPublishTargetGivenUrlWithoutPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io";
    assertThat(EventsCollectionConfigExtractor.extractPublishTarget(managerUrl)).isEqualTo("pr.harness.io");
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractPublishAuthorityGivenUrlWithPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io/ccm";
    assertThat(EventsCollectionConfigExtractor.extractPublishAuthority(managerUrl))
        .isEqualTo("ccm-events-pr.harness.io");
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractPublishAuthorityGivenUrlWithoutPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io";
    assertThat(EventsCollectionConfigExtractor.extractPublishAuthority(managerUrl)).isEqualTo("events-pr.harness.io");
  }
}