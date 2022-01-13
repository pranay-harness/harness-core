/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.MATT;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
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

@OwnedBy(PIPELINE)
public class NGTriggerServiceTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @InjectMocks NGTriggerServiceImpl ngTriggerServiceImpl;

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testCronTriggerFailure() {
    TriggerDetails triggerDetails =
        TriggerDetails.builder()
            .ngTriggerEntity(NGTriggerEntity.builder().identifier("id").name("name").build())
            .ngTriggerConfigV2(
                NGTriggerConfigV2.builder()
                    .source(NGTriggerSourceV2.builder()
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testEmptyIdentifierTriggerFailure() {
    NGTriggerEntity ngTriggerEntity = NGTriggerEntity.builder().identifier("").name("name").build();
    TriggerDetails triggerDetails = TriggerDetails.builder().ngTriggerEntity(ngTriggerEntity).build();

    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .hasMessage("Identifier can not be empty");

    ngTriggerEntity.setIdentifier(null);
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .hasMessage("Identifier can not be empty");

    ngTriggerEntity.setIdentifier("  ");
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .hasMessage("Identifier can not be empty");

    ngTriggerEntity.setIdentifier("a1");
    ngTriggerEntity.setName("");
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> ngTriggerServiceImpl.validateTriggerConfig(triggerDetails))
        .hasMessage("Name can not be empty");
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testCronTrigger() {
    TriggerDetails triggerDetails =
        TriggerDetails.builder()
            .ngTriggerEntity(NGTriggerEntity.builder().identifier("id").name("name").build())
            .ngTriggerConfigV2(
                NGTriggerConfigV2.builder()
                    .source(NGTriggerSourceV2.builder()
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
