/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import static io.harness.mongo.ClassRefactoringManager.movementsToMyModule;
import static io.harness.mongo.HObjectFactory.checkRegisteredClasses;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.rule.Owner;
import io.harness.waiter.OldNotifyCallback;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentInfo;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.common.SampleElement;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.service.impl.WorkflowExecutionUpdateFake;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElement;
import software.wings.sm.State;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutionCallbackMock;
import software.wings.sm.StateMachineTestBase.StateAsync;
import software.wings.sm.StateMachineTestBase.StateSync;
import software.wings.sm.StepExecutionSummary;
import software.wings.verification.CVConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
@Slf4j
public class ManagerMorphiaRegistrarTest extends WingsBaseTest {
  @Inject @Named("morphiaInterfaceImplementersClasses") Map<String, Class> morphiaInterfaceImplementers;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @Ignore("TODO: This test is failing in bazel. Changes are required from the owner to make it work in bazel")
  public void testManagerImplementationClasses() {
    Map<String, Class> classes = new HashedMap(morphiaInterfaceImplementers);

    classes.put("software.wings.common.PartitionProcessorTest$SampleElement", SampleElement.class);
    classes.put("software.wings.sm.StateMachineTest$StateSync", StateSync.class);
    classes.put("software.wings.sm.StateMachineTest$StateAsync", StateAsync.class);
    classes.put("software.wings.sm.StateMachineExecutionCallbackMock", StateMachineExecutionCallbackMock.class);
    classes.put("software.wings.service.impl.WorkflowExecutionUpdateFake", WorkflowExecutionUpdateFake.class);

    final Set<Class> unwanted = checkRegisteredClasses(ImmutableSet.<Class>builder()
                                                           .add(CommandUnit.class)
                                                           .add(ContextElement.class)
                                                           .add(DataCollectionInfo.class)
                                                           .add(SecretManagerConfig.class)
                                                           .add(SettingValue.class)
                                                           .add(State.class)
                                                           .add(StateMachineExecutionCallback.class)
                                                           .add(StepExecutionSummary.class)
                                                           .add(InfraMappingInfrastructureProvider.class)
                                                           .add(LogDataCollectionInfoV2.class)
                                                           .add(OldNotifyCallback.class)
                                                           .add(CVConfiguration.class)
                                                           .add(InstanceInfo.class)
                                                           .add(Cluster.class)
                                                           .add(DeploymentInfo.class)
                                                           .build(),
        classes);

    assertThat(unwanted).isEmpty();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @Ignore("TODO: This test is failing in bazel. Changes are required from the owner to make it work in bazel")
  public void testManagerKnownMovements() {
    ImmutableMap<Object, Object> known = ImmutableMap.builder().build();

    Map<String, String> myModuleMovements = movementsToMyModule(this.getClass(), morphiaInterfaceImplementers);

    assertThat(myModuleMovements).isEqualTo(known);
  }
}
