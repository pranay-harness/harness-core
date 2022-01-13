/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.template.Template;
import software.wings.beans.template.artifactsource.ArtifactSource;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.yaml.templatelibrary.ArtifactSourceTemplateYaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton
public class ArtifactSourceTemplateYamlHandler extends TemplateLibraryYamlHandler<ArtifactSourceTemplateYaml> {
  @Inject CustomArtifactSourceYamlHandler customArtifactSourceYamlHandler;

  @Override
  protected void setBaseTemplate(Template template, ChangeContext<ArtifactSourceTemplateYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    ArtifactSourceTemplate artifactSource =
        ArtifactSourceTemplate.builder().artifactSource(toBean(changeContext, changeSetContext)).build();
    template.setTemplateObject(artifactSource);
  }

  @Override
  public ArtifactSourceTemplateYaml toYaml(Template bean, String appId) {
    ArtifactSourceTemplate artifactSource = (ArtifactSourceTemplate) bean.getTemplateObject();

    BaseYamlHandler yamlHandler = getYamlHandler(artifactSource.getArtifactSource());
    ArtifactSourceTemplateYaml artifactSourceYaml =
        (ArtifactSourceTemplateYaml) yamlHandler.toYaml(artifactSource.getArtifactSource(), appId);
    super.toYaml(artifactSourceYaml, bean);
    return artifactSourceYaml;
  }

  @Override
  public Class getYamlClass() {
    return ArtifactSourceTemplateYaml.class;
  }

  private ArtifactSource toBean(
      ChangeContext<ArtifactSourceTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    ChangeContext<ArtifactSourceTemplateYaml> clonedChangeContext =
        cloneFileChangeContext(changeContext, changeContext.getYaml()).build();
    BaseYamlHandler yamlHandler = getYamlHandler(changeContext.getYaml());
    ArtifactSource artifactSource = null;
    try {
      artifactSource = (ArtifactSource) yamlHandler.upsertFromYaml(clonedChangeContext, changeSetContext);
    } catch (Exception e) {
      throw new InvalidRequestException(e.getMessage(), e);
    }
    return artifactSource;
  }

  private BaseYamlHandler getYamlHandler(ArtifactSourceTemplateYaml yaml) {
    // Logic can be added here if more Artifact come in later.
    return customArtifactSourceYamlHandler;
  }

  private BaseYamlHandler getYamlHandler(ArtifactSource bean) {
    // Logic can be added here if more Artifact come in later.
    return customArtifactSourceYamlHandler;
  }
}
