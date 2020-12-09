package io.harness.beans.serializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CiBeansTestBase;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionProtobufSerializerTest extends CiBeansTestBase {
  @Inject ProtobufSerializer<ExecutionElement> executionProtobufSerializer;

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void serialize() {
    StepElement run1 = StepElement.builder()
                           .name("run1")
                           .identifier("runId1")
                           .stepSpecType(RunStepInfo.builder()
                                             .name("run1")
                                             .callbackId("callbackId")
                                             .identifier("runId1")
                                             .command(Arrays.asList("run1c1", "run1c2"))
                                             .port(8000)
                                             .build())
                           .build();
    StepElement run2 = StepElement.builder()
                           .stepSpecType(RunStepInfo.builder()
                                             .name("run2")
                                             .callbackId("callbackId")
                                             .identifier("runId2")
                                             .command(Arrays.asList("run2c1", "run2c2"))
                                             .port(8001)
                                             .build())
                           .build();
    StepElement plugin1 = StepElement.builder()
                              .stepSpecType(PluginStepInfo.builder()
                                                .name("plugin1")
                                                .callbackId("callbackId")
                                                .identifier("plugin1")
                                                .image("git")
                                                .port(8002)
                                                .build())
                              .build();
    ExecutionElement execution = ExecutionElement.builder().steps(Arrays.asList(run1, run2, plugin1)).build();

    String steps = executionProtobufSerializer.serialize(execution);
    byte[] bytes = Base64.decodeBase64(steps);
    io.harness.product.ci.engine.proto.Execution parsedExecution =
        io.harness.product.ci.engine.proto.Execution.parseFrom(bytes);

    assertThat(parsedExecution.getStepsList()).hasSize(3);

    assertThat(parsedExecution.getStepsList().get(0).getUnit().getRun().getCommands(0)).isEqualTo("run1c1");
    assertThat(parsedExecution.getStepsList().get(0).getUnit().getRun().getCommands(1)).isEqualTo("run1c2");

    assertThat(parsedExecution.getStepsList().get(1).getUnit().getRun().getCommands(0)).isEqualTo("run2c1");
    assertThat(parsedExecution.getStepsList().get(1).getUnit().getRun().getCommands(1)).isEqualTo("run2c2");

    assertThat(parsedExecution.getStepsList().get(2).getUnit().getPlugin().getImage()).isEqualTo("git");
  }
}
