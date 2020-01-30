package software.wings.service.impl.yaml.handler.templatelibrary;

import com.google.inject.Singleton;

import io.harness.exception.GeneralException;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.yaml.templatelibrary.ArtifactSourceTemplateYaml;
import software.wings.yaml.templatelibrary.CustomArtifactSourceTemplateYaml;

import java.util.List;

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
    throw new GeneralException("Not implemented");
  }
}
