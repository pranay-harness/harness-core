package io.harness.ngtriggers.helpers;

import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_PAYLOAD;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_TRIGGER_FOUND_FOR_REPO;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_REPO;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_DID_NOT_EXECUTE;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookEventResponseHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testIsFinalStatusAnEvent() {
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(INVALID_PAYLOAD)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(NO_MATCHING_TRIGGER_FOR_REPO)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(NO_ENABLED_TRIGGER_FOUND_FOR_REPO)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(INVALID_RUNTIME_INPUT_YAML)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(TARGET_DID_NOT_EXECUTE)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(TARGET_EXECUTION_REQUESTED)).isTrue();
  }
}
