/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.k8s.client;

import static io.harness.annotations.dev.HarnessModule._930_DELEGATE_TASKS;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.apiclient.KubernetesApiClientFactoryModule;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;
import io.harness.rule.Owner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
@TargetModule(_930_DELEGATE_TASKS)
public class KubernetesApiClientFactoryModuleTest extends CategoryTest {
  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testApiClientFactoryModule() {
    List<Module> modules = new ArrayList<>();
    modules.add(KubernetesApiClientFactoryModule.getInstance());

    Injector injector = Guice.createInjector(modules);

    ApiClientFactory apiClientFactory = injector.getInstance(ApiClientFactory.class);
    assertThat(apiClientFactory).isNotNull();
    assertThat(apiClientFactory).isInstanceOf(ApiClientFactoryImpl.class);
  }
}
