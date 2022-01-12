/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.artifactstream;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class CustomArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<CustomArtifactStream.Yaml, CustomArtifactStream> {
  @Override
  protected CustomArtifactStream getNewArtifactStreamObject() {
    return new CustomArtifactStream();
  }

  @Override
  public Yaml toYaml(CustomArtifactStream bean, String appId) {
    CustomArtifactStream.Yaml yaml = CustomArtifactStream.Yaml.builder().build();
    super.toYaml(yaml, bean);
    if (bean.getTemplateUuid() == null) {
      yaml.setScripts(bean.getScripts());
    }
    yaml.setDelegateTags(bean.getTags());
    return yaml;
  }

  @Override
  protected void toBean(CustomArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setScripts(yaml.getScripts());
    bean.setTags(yaml.getDelegateTags());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
