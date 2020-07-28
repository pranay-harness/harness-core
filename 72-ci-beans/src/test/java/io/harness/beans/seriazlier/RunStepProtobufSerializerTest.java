package io.harness.beans.seriazlier;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.engine.proto.Step;
import io.harness.rule.Owner;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class RunStepProtobufSerializerTest extends CIBeansTest {
  public static final String RUN_STEP = "run-step";
  public static final String RUN_STEP_ID = "run-step-id";
  public static final String MVN_CLEAN_INSTALL = "mvn clean install";
  public static final String OUTPUT = "output";
  public static final int TIMEOUT = 100;
  public static final int RETRY = 2;
  @Inject ProtobufSerializer<RunStepInfo> protobufSerializer;
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeRunStep() throws InvalidProtocolBufferException {
    RunStepInfo runStepInfo = RunStepInfo.builder()
                                  .name(RUN_STEP)
                                  .identifier(RUN_STEP_ID)
                                  .retry(RETRY)
                                  .timeout(TIMEOUT)
                                  .command(Arrays.asList(MVN_CLEAN_INSTALL))
                                  .output(Arrays.asList(OUTPUT))

                                  .build();
    String serialize = protobufSerializer.serialize(runStepInfo);
    Step runStep = Step.parseFrom(Base64.decodeBase64(serialize));
    assertThat(runStep.getUnit().getId()).isEqualTo(RUN_STEP_ID);
    assertThat(runStep.getUnit().getDisplayName()).isEqualTo(RUN_STEP);
    assertThat(runStep.getUnit().getRun().getContext().getNumRetries()).isEqualTo(RETRY);
    assertThat(runStep.getUnit().getRun().getContext().getExecutionTimeoutSecs()).isEqualTo(TIMEOUT);
    assertThat(runStep.getUnit().getRun().getCommands(0)).isEqualTo(MVN_CLEAN_INSTALL);
  }
}