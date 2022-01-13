/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;

import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.yaml.templatelibrary.ArtifactSourceTemplateYaml;
import software.wings.yaml.templatelibrary.CustomArtifactSourceTemplateYaml;

import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton
public class CustomArtifactSourceYamlHandler
    extends BaseYamlHandler<CustomArtifactSourceTemplateYaml, CustomArtifactSourceTemplate> {
  @Override
  public void delete(ChangeContext<CustomArtifactSourceTemplateYaml> changeContext) {
    throw new GeneralException("Not implemented");
  }

  @Override
  public CustomArtifactSourceTemplateYaml toYaml(CustomArtifactSourceTemplate bean, String appId) {
    return CustomArtifactSourceTemplateYaml.builder()
        .script(bean.getScript())
        .timeout(bean.getTimeoutSeconds())
        .customRepositoryMapping(bean.getCustomRepositoryMapping())
        .build();
  }

  @Override
  public CustomArtifactSourceTemplate upsertFromYaml(
      ChangeContext<CustomArtifactSourceTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    ArtifactSourceTemplateYaml artifactSourceTemplateYaml = changeContext.getYaml();
    CustomArtifactSourceTemplateYaml yaml = (CustomArtifactSourceTemplateYaml) artifactSourceTemplateYaml;
    return CustomArtifactSourceTemplate.builder()
        .timeoutSeconds(yaml.getTimeout())
        .script(yaml.getScript())
        .customRepositoryMapping(yaml.getCustomRepositoryMapping())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return CustomArtifactSourceTemplateYaml.class;
  }

  @Override
  public CustomArtifactSourceTemplate get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
