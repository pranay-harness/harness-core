package io.harness.beans.seriazlier;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.Execution;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class ExecutionProtobufSerializerTest extends CIBeansTest {
  @Inject ProtobufSerializer<Execution> executionProtobufSerializer;

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void serialize() {
    RunStepInfo run1 = RunStepInfo.builder()
                           .displayName("run1")
                           .identifier("runId1")
                           .run(RunStepInfo.Run.builder().command(Arrays.asList("run1c1", "run1c2")).build())
                           .build();
    RunStepInfo run2 = RunStepInfo.builder()
                           .displayName("run2")
                           .identifier("runId2")
                           .run(RunStepInfo.Run.builder().command(Arrays.asList("run2c1", "run2c2")).build())
                           .build();
    Execution execution = Execution.builder().steps(Arrays.asList(run1, run2)).build();

    String steps = executionProtobufSerializer.serialize(execution);
    byte[] bytes = Base64.decodeBase64(steps);
    io.harness.product.ci.engine.proto.Execution parsedExecution =
        io.harness.product.ci.engine.proto.Execution.parseFrom(bytes);

    assertThat(parsedExecution.getStepsList()).hasSize(2);

    assertThat(parsedExecution.getStepsList().get(0).getRun().getCommands(0)).isEqualTo("run1c1");
    assertThat(parsedExecution.getStepsList().get(0).getRun().getCommands(1)).isEqualTo("run1c2");

    assertThat(parsedExecution.getStepsList().get(1).getRun().getCommands(0)).isEqualTo("run2c1");
    assertThat(parsedExecution.getStepsList().get(1).getRun().getCommands(1)).isEqualTo("run2c2");
  }
}