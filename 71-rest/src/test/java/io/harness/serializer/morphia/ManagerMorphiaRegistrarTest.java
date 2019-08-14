package io.harness.serializer.morphia;

import static io.harness.mongo.HObjectFactory.checkRegisteredClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.HObjectFactory.NotFoundClass;
import io.harness.reflection.CodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import software.wings.WingsBaseTest;
import software.wings.beans.alert.AlertData;
import software.wings.beans.command.CommandUnit;
import software.wings.common.PartitionProcessorTest.SampleElement;
import software.wings.integration.common.MongoDBTest.MongoEntity;
import software.wings.integration.dl.PageRequestTest.Dummy;
import software.wings.service.impl.WorkflowExecutionUpdateFake;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElement;
import software.wings.sm.State;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutionCallbackMock;
import software.wings.sm.StateMachineTest.StateAsync;
import software.wings.sm.StateMachineTest.StateSync;
import software.wings.sm.StepExecutionSummary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ManagerMorphiaRegistrarTest extends WingsBaseTest {
  @Inject HObjectFactory objectFactory;
  @Inject @Named("morphiaClasses") Set<Class> morphiaClasses;

  @Test
  @Category(UnitTests.class)
  public void testManagerClassesModule() {
    final Set<Class> classes = new HashSet<>();
    new ManagerMorphiaRegistrar().registerClasses(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(ManagerMorphiaRegistrar.class), classes);
  }

  @Test
  @Category(UnitTests.class)
  public void testManagerSearchAndList() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");

    final HashSet<Class> classes = new HashSet<>(morphiaClasses);
    classes.add(Dummy.class);
    classes.add(MongoEntity.class);

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!classes.contains(cls.getClazz())) {
        logger.error(cls.getClazz().toString());
        success = false;
      }
    }

    assertThat(success).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testManagerImplementationClasses() {
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
                                                           .add(DataCollectionInfo.class)
                                                           .add(SettingValue.class)
                                                           .add(State.class)
                                                           .add(StateMachineExecutionCallback.class)
                                                           .add(StepExecutionSummary.class)
                                                           .build(),
        classes);

    assertThat(unwanted).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testManagerImplementationClassesModule() {
    final Map<String, Class> map = new HashMap<>();
    new ManagerMorphiaRegistrar().registerImplementationClasses(map);

    Set<Class> classes = new HashSet<>(map.values());
    classes.remove(NotFoundClass.class);

    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(ManagerMorphiaRegistrar.class), classes);
  }
}
