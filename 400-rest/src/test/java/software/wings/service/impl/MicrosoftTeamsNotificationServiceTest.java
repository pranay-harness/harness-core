package software.wings.service.impl;

import static io.harness.rule.OwnerRule.MEHUL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.MicrosoftTeamsNotificationService;

import com.google.inject.Inject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MicrosoftTeamsNotificationServiceTest extends WingsBaseTest {
  @Inject private MicrosoftTeamsNotificationService microsoftTeamsNotificationService;

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  @Ignore("Not working")
  public void testSendMessageSuccessful() {
    assertThat(microsoftTeamsNotificationService.sendMessage("some-message", "https://app.harness.io"))
        .isNotEqualTo(200);
  }
}
