package io.harness.ngtriggers.service;

import static io.harness.rule.OwnerRule.MATT;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.service.impl.NGTriggerServiceImpl;
import io.harness.rule.Owner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class NGTriggerServiceTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @InjectMocks NGTriggerServiceImpl ngTriggerServiceImpl;

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testCronTriggerFailure() {
    TriggerDetails triggerDetails =
        TriggerDetails.builder()
            .ngTriggerConfig(NGTriggerConfig.builder()
                                 .source(NGTriggerSource.builder()
                                             .type(NGTriggerType.SCHEDULED)
                                             .spec(ScheduledTriggerConfig.builder()
                                                       .type("Cron")
                                                       .spec(CronTriggerSpec.builder().expression("not a cron").build())
                                                       .build())
                                             .build())
                                 .build())
            .build();
    try {
      ngTriggerServiceImpl.validateTriggerConfig(triggerDetails);
      fail("bad cron not caught");
    } catch (Exception e) {
      assertThat(e instanceof IllegalArgumentException).isTrue();
    }
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testCronTrigger() {
    TriggerDetails triggerDetails =
        TriggerDetails.builder()
            .ngTriggerConfig(NGTriggerConfig.builder()
                                 .source(NGTriggerSource.builder()
                                             .type(NGTriggerType.SCHEDULED)
                                             .spec(ScheduledTriggerConfig.builder()
                                                       .type("Cron")
                                                       .spec(CronTriggerSpec.builder().expression("20 4 * * *").build())
                                                       .build())
                                             .build())
                                 .build())
            .build();
    ngTriggerServiceImpl.validateTriggerConfig(triggerDetails);
  }
}
