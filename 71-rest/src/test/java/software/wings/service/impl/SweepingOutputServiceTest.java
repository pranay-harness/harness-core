package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import io.harness.serializer.KryoUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

public class SweepingOutputServiceTest extends WingsBaseTest {
  @Inject SweepingOutputService sweepingOutputService;
  @Inject HPersistence persistence;

  @Test
  @Owner(developers = PRASHANT, intermittent = true)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldGetInstanceId() {
    persistence.ensureIndex(SweepingOutputInstance.class);

    final SweepingOutputInstanceBuilder builder = SweepingOutputInstance.builder()
                                                      .name("jenkins")
                                                      .appId(generateUuid())
                                                      .pipelineExecutionId(generateUuid())
                                                      .workflowExecutionId(generateUuid())
                                                      .output(KryoUtils.asDeflatedBytes(ImmutableMap.of("foo", "bar")));

    sweepingOutputService.save(builder.uuid(generateUuid()).build());
    assertThatThrownBy(() -> sweepingOutputService.save(builder.uuid(generateUuid()).build()))
        .isInstanceOf(WingsException.class);
  }
}
