package io.harness.beans.serializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CiBeansTestBase;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.rule.Owner;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RunStepProtobufSerializerTest extends CiBeansTestBase {
  public static final String RUN_STEP = "run-step";
  public static final String RUN_STEP_ID = "run-step-id";
  public static final String MVN_CLEAN_INSTALL = "mvn clean install";
  public static final String OUTPUT = "output";
  public static final int TIMEOUT = 100;
  public static final String CALLBACK_ID = "callbackId";
  public static final int RETRY = 2;
  public static final Integer PORT = 8000;
  @Inject ProtobufStepSerializer<RunStepInfo> protobufSerializer;

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
    StepElement stepElement = StepElement.builder().type("run").stepSpecType(runStepInfo).build();

    runStepInfo.setCallbackId(CALLBACK_ID);
    runStepInfo.setPort(PORT);
    String serialize = protobufSerializer.serializeToBase64(stepElement);
    UnitStep runStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));
    assertThat(runStep.getId()).isEqualTo(RUN_STEP_ID);
    assertThat(runStep.getDisplayName()).isEqualTo(RUN_STEP);
    assertThat(runStep.getRun().getContext().getNumRetries()).isEqualTo(RETRY);
    assertThat(runStep.getRun().getContext().getExecutionTimeoutSecs()).isEqualTo(TIMEOUT);
    assertThat(runStep.getRun().getCommands(0)).isEqualTo(MVN_CLEAN_INSTALL);
    assertThat(runStep.getRun().getContainerPort()).isEqualTo(PORT);
  }
}
