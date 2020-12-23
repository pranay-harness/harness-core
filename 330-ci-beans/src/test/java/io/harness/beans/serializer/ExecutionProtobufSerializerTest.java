package io.harness.beans.serializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import io.harness.CiBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionProtobufSerializerTest extends CiBeansTestBase {
  @Inject ProtobufSerializer<ExecutionElementConfig> executionProtobufSerializer;

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void serialize() {
    //    StepElementConfig run1 = StepElementConfig.builder()
    //                           .name("run1")
    //                           .identifier("runId1")
    //                           .stepSpecType(RunStepInfo.builder()
    //                                             .name("run1")
    //                                             .callbackId("callbackId")
    //                                             .identifier("runId1")
    //                                             .command("run1c1")
    //                                             .port(8000)
    //                                             .build())
    //                           .build();
    //    StepElementConfig run2 = StepElementConfig.builder()
    //                           .stepSpecType(RunStepInfo.builder()
    //                                             .name("run2")
    //                                             .callbackId("callbackId")
    //                                             .identifier("runId2")
    //                                             .command("run2c1")
    //                                             .port(8001)
    //                                             .build())
    //                           .build();
    //    StepElementConfig plugin1 = StepElementConfig.builder()
    //                              .stepSpecType(PluginStepInfo.builder()
    //                                                .name("plugin1")
    //                                                .callbackId("callbackId")
    //                                                .identifier("plugin1")
    //                                                .image("git")
    //                                                .port(8002)
    //                                                .build())
    //                              .build();
    //    ExecutionElementConfig execution = ExecutionElementConfig.builder().steps(Arrays.asList(run1, run2,
    //    plugin1)).build();
    //
    //    String steps = executionProtobufSerializer.serialize(execution);
    //    byte[] bytes = Base64.decodeBase64(steps);
    //    io.harness.product.ci.engine.proto.Execution parsedExecution =
    //        io.harness.product.ci.engine.proto.Execution.parseFrom(bytes);
    //
    //    assertThat(parsedExecution.getStepsList()).hasSize(3);
    //
    //    assertThat(parsedExecution.getStepsList().get(0).getUnit().getRun().getCommand()).isEqualTo("run1c1");
    //
    //    assertThat(parsedExecution.getStepsList().get(1).getUnit().getRun().getCommand()).isEqualTo("run2c1");
    //
    //    assertThat(parsedExecution.getStepsList().get(2).getUnit().getPlugin().getImage()).isEqualTo("git");
  }
}
