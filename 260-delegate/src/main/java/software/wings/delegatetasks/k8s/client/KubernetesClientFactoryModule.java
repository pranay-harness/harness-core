/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.k8s.client;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.AbstractModule;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class KubernetesClientFactoryModule extends AbstractModule {
  private static KubernetesClientFactoryModule instance;

  private KubernetesClientFactoryModule() {}

  public static KubernetesClientFactoryModule getInstance() {
    if (instance == null) {
      instance = new KubernetesClientFactoryModule();
    }

    return instance;
  }

  @Override
  protected void configure() {
    bind(KubernetesClientFactory.class).to(HarnessKubernetesClientFactory.class);
  }
}
