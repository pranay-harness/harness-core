/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class NameValuePairYamlHandler extends BaseYamlHandler<NameValuePair.Yaml, NameValuePair> {
  private NameValuePair toBean(ChangeContext<Yaml> changeContext) {
    NameValuePair.Yaml yaml = changeContext.getYaml();
    return NameValuePair.builder().name(yaml.getName()).value(yaml.getValue()).valueType(yaml.getValueType()).build();
  }

  @Override
  public NameValuePair.Yaml toYaml(NameValuePair bean, String appId) {
    return NameValuePair.Yaml.builder()
        .name(bean.getName())
        .value(bean.getValue())
        .valueType(bean.getValueType())
        .build();
  }

  @Override
  public NameValuePair upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return NameValuePair.Yaml.class;
  }

  @Override
  public NameValuePair get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    // Do nothing
  }
}
