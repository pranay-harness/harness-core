package io.harness.serializer.morphia;

import static io.harness.mongo.HObjectFactory.checkRegisteredClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.mongo.HObjectFactory;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.alert.AlertData;
import software.wings.beans.command.CommandUnit;
import software.wings.common.PartitionProcessorTest.SampleElement;
import software.wings.service.impl.WorkflowExecutionUpdateFake;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElement;
import software.wings.sm.State;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutionCallbackMock;
import software.wings.sm.StateMachineTest.StateAsync;
import software.wings.sm.StateMachineTest.StateSync;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;
import java.util.Set;

public class ManagerMorphiaRegistrarTest extends WingsBaseTest {
  @Inject HObjectFactory objectFactory;

  @Test
  @Category(UnitTests.class)
  public void testRegister() {
    Map<String, Class> classes = new HashedMap(objectFactory.getMorphiaInterfaceImplementers());

    classes.put("software.wings.common.PartitionProcessorTest$SampleElement", SampleElement.class);
    classes.put("software.wings.sm.StateMachineTest$StateSync", StateSync.class);
    classes.put("software.wings.sm.StateMachineTest$StateAsync", StateAsync.class);
    classes.put("software.wings.sm.StateMachineExecutionCallbackMock", StateMachineExecutionCallbackMock.class);
    classes.put("software.wings.service.impl.WorkflowExecutionUpdateFake", WorkflowExecutionUpdateFake.class);

    final Set<Class> unwanted = checkRegisteredClasses(ImmutableSet.<Class>builder()
                                                           .add(AlertData.class)
                                                           .add(CommandUnit.class)
                                                           .add(ContextElement.class)
                                                           .add(SettingValue.class)
                                                           .add(State.class)
                                                           .add(StateMachineExecutionCallback.class)
                                                           .add(StepExecutionSummary.class)
                                                           .build(),
        classes);

    assertThat(unwanted).isEmpty();
  }
}
