package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.RealMongo;
import io.harness.serializer.KryoUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.SweepingOutputBuilder;
import software.wings.service.intfc.SweepingOutputService;

public class SweepingOutputServiceTest extends WingsBaseTest {
  @Inject SweepingOutputService sweepingOutputService;
  @Inject HPersistence persistence;

  @Test
  @RealMongo
  public void shouldGetInstanceId() {
    persistence.ensureIndex(SweepingOutput.class);

    final SweepingOutputBuilder builder = SweepingOutput.builder()
                                              .name("jenkins")
                                              .appId(generateUuid())
                                              .pipelineExecutionId(generateUuid())
                                              .workflowExecutionId(generateUuid())
                                              .output(KryoUtils.asDeflatedBytes(ImmutableMap.of("foo", "bar")));

    sweepingOutputService.save(builder.build());
    assertThatThrownBy(() -> sweepingOutputService.save(builder.build())).isInstanceOf(WingsException.class);
  }
}
