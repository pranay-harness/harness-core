/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.core.k8s;

import static io.harness.rule.OwnerRule.MARKO;

import io.harness.category.element.FunctionalTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.rule.Owner;

import software.wings.beans.bash.ShellScriptParameters;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8STaskRunnerTest {
  private K8STaskRunner underTest;

  @Before
  public void setup() throws IOException {
    final var delegateConfig = DelegateConfiguration.builder().accountId("accountId").build();
    underTest = new K8STaskRunner(delegateConfig, Config.defaultClient());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  @Ignore("This test creates K8S resources. Good for local testing, but don't want dangling resources on every PR")
  public void testCreateK8STask() throws IOException, ApiException {
    final var taskId = UUID.randomUUID().toString();
    final var taskPackage = DelegateTaskPackage.builder().delegateTaskId(taskId).data(createDummyTaskData()).build();
    underTest.launchTask(taskPackage);
    underTest.cleanupTaskData(taskId);
  }

  private static TaskData createDummyTaskData() {
    final Map<String, String> vars = ImmutableMap.of("key1", "va1", "key2", "val2");
    final var shellScriptParameters =
        new ShellScriptParameters("actId", "some,vars", "another,secret", "my super \n scrupt", 1000, "accId", "appId",
            "/root/not", Collections.emptyMap(), Collections.emptyMap(), vars);
    final var objects = new Object[] {shellScriptParameters};
    return TaskData.builder().parameters(objects).build();
  }
}
