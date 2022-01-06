/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.deploymentspec.container;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.container.StorageConfiguration.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class StorageConfigurationYamlHandler extends BaseYamlHandler<Yaml, StorageConfiguration> {
  @Override
  public Yaml toYaml(StorageConfiguration storageConfiguration, String appId) {
    return Yaml.builder()
        .containerPath(storageConfiguration.getContainerPath())
        .hostSourcePath(storageConfiguration.getHostSourcePath())
        .readonly(storageConfiguration.isReadonly())
        .build();
  }

  @Override
  public StorageConfiguration upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  private StorageConfiguration toBean(ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    return StorageConfiguration.builder()
        .containerPath(yaml.getContainerPath())
        .hostSourcePath(yaml.getHostSourcePath())
        .readonly(yaml.isReadonly())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public StorageConfiguration get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    // Do nothing
  }
}
