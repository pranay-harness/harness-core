package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordKeys;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SLIRecordServiceImplTest extends CvNextGenTestBase {
  @Inject private SLIRecordService sliRecordService;
  @Inject private HPersistence hPersistence;
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_Success() {
    String verificationTaskId = generateUuid();
    sliRecordService.create(SLIRecord.builder()
                                .sliState(SLIState.BAD)
                                .runningBadCount(1)
                                .epochMinute(3)
                                .runningGoodCount(0)
                                .verificationTaskId(verificationTaskId)
                                .build());
    sliRecordService.create(SLIRecord.builder()
                                .sliState(SLIState.BAD)
                                .runningBadCount(3)
                                .epochMinute(5)
                                .runningGoodCount(0)
                                .verificationTaskId(verificationTaskId)
                                .build());
    assertThat(hPersistence.createQuery(SLIRecord.class).field(SLIRecordKeys.epochMinute).mod(5, 0).asList())

        .hasSize(1);
  }
}