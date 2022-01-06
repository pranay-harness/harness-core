/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.deploymentspec.container;

import software.wings.api.DeploymentType;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesContainerTask.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class KubernetesContainerTaskYamlHandler extends ContainerTaskYamlHandler<Yaml, KubernetesContainerTask> {
  @Override
  public Yaml toYaml(KubernetesContainerTask bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public KubernetesContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.KUBERNETES.name());
  }

  @Override
  protected KubernetesContainerTask createNewContainerTask() {
    return new KubernetesContainerTask();
  }
}
