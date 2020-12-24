package io.harness.beans.serializer;

import static io.harness.rule.OwnerRule.SHUBHAM;

import io.harness.CiBeansTestBase;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PluginStepProtobufSerializerTest extends CiBeansTestBase {
  public static final String PLUGIN_STEP = "plugin-step";
  public static final String PLUGIN_STEP_ID = "run-step-id";
  public static final String PLUGIN_IMAGE = "plugins/git";
  public static final int TIMEOUT = 100;
  public static final String CALLBACK_ID = "callbackId";
  public static final int RETRY = 1;
  public static final Integer PORT = 8000;
  @Inject ProtobufStepSerializer<PluginStepInfo> protobufSerializer;

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void shouldSerializeRunStep() throws InvalidProtocolBufferException {
    PluginStepInfo pluginStepInfo = PluginStepInfo.builder()
                                        .name(PLUGIN_STEP)
                                        .identifier(PLUGIN_STEP_ID)
                                        .retry(RETRY)
                                        .timeout(TIMEOUT)
                                        .image(PLUGIN_IMAGE)
                                        .build();
    pluginStepInfo.setCallbackId(CALLBACK_ID);
    pluginStepInfo.setPort(PORT);
    StepElementConfig stepElement = StepElementConfig.builder()
                                        .identifier(PLUGIN_STEP_ID)
                                        .name(PLUGIN_STEP)
                                        .type("plugin")
                                        .stepSpecType(pluginStepInfo)
                                        .build();

    //    String serialize = protobufSerializer.serializeToBase64(stepElement);
    //    UnitStep pluginStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));
    //    assertThat(pluginStep.getId()).isEqualTo(PLUGIN_STEP_ID);
    //    assertThat(pluginStep.getDisplayName()).isEqualTo(PLUGIN_STEP);
    //    assertThat(pluginStep.getPlugin().getContext().getNumRetries()).isEqualTo(RETRY);
    //    assertThat(pluginStep.getPlugin().getContext().getExecutionTimeoutSecs()).isEqualTo(TIMEOUT);
    //    assertThat(pluginStep.getPlugin().getImage()).isEqualTo(PLUGIN_IMAGE);
    //    assertThat(pluginStep.getPlugin().getContainerPort()).isEqualTo(PORT);
  }
}
