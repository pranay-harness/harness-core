package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.template.TemplateType.ARTIFACT_SOURCE;
import static software.wings.common.TemplateConstants.LATEST_TAG;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.HIterator;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.List;

@Singleton
public class ArtifactSourceTemplateProcessor extends AbstractTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactSourceTemplateProcessor.class);
  @Inject ArtifactStreamService artifactStreamService;
  @Inject TemplateHelper templateHelper;

  @Override
  public TemplateType getTemplateType() {
    return ARTIFACT_SOURCE;
  }

  @Override
  public Template process(Template template) {
    template.setType(getTemplateType().name());
    List<String> keywords = generateKeyword(template);
    if (EmptyPredicate.isNotEmpty(keywords)) {
      template.setKeywords(keywords);
    }

    ensureDefaults(template);
    return template;
  }

  private List<String> generateKeyword(Template template) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
    if (artifactSourceTemplate.getArtifactSource() instanceof CustomArtifactSourceTemplate) {
      return asList(ArtifactStreamType.CUSTOM.name());
    }
    return null;
  }

  private void ensureDefaults(Template template) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
    if (artifactSourceTemplate.getArtifactSource() instanceof CustomArtifactSourceTemplate) {
      if (isEmpty(((CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource()).getTimeoutSeconds())
          || ((CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource())
                 .getTimeoutSeconds()
                 .equals("0")) {
        ((CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource()).setTimeoutSeconds("60");
      }
    }
  }

  @Override
  public void updateLinkedEntities(Template template) {
    Template savedTemplate = templateService.get(template.getUuid());
    if (savedTemplate == null) {
      logger.info("Template {} was deleted. Not updating linked entities", template.getUuid());
      return;
    }

    // Read all the service commands that references the given
    try (HIterator<ArtifactStream> iterator =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)
                                 .filter(ArtifactStream.TEMPATE_UUID_KEY, template.getUuid())
                                 .fetch())) {
      while (iterator.hasNext()) {
        ArtifactStream artifactStream = iterator.next();
        try {
          String templateVersion = artifactStream.getTemplateVersion();
          if (templateVersion == null || templateVersion.equalsIgnoreCase(LATEST_TAG)) {
            logger.info(format("Updating the linked artifact stream with id %s", artifactStream.getUuid()));
            ArtifactStream entityFromTemplate = constructEntityFromTemplate(template);
            updateEntity(entityFromTemplate, artifactStream);
            artifactStreamService.update(artifactStream);
            logger.info("Linked artifact stream with id %s updated", artifactStream.getUuid());
          } else {
            logger.info("The linked template is not the latest. So, not updating it");
          }
        } catch (Exception e) {
          logger.warn(format("Failed to update the linked Artifact Stream %s", artifactStream.getUuid()), e);
        }
      }
    }
  }

  public ArtifactStream constructEntityFromTemplate(Template template) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
    if (artifactSourceTemplate.getArtifactSource() instanceof CustomArtifactSourceTemplate) {
      CustomArtifactSourceTemplate customArtifactSourceTemplate =
          (CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource();
      CustomArtifactStream customArtifactStream =
          CustomArtifactStream.builder()
              .scripts(asList(CustomArtifactStream.Script.builder()
                                  .scriptString(customArtifactSourceTemplate.getScript())
                                  .timeout(customArtifactSourceTemplate.getTimeoutSeconds())
                                  .customRepositoryMapping(customArtifactSourceTemplate.getCustomRepositoryMapping())
                                  .build()))
              .build();
      customArtifactStream.setTemplateUuid(template.getUuid());
      customArtifactStream.setTemplateVersion(String.valueOf(template.getVersion()));
      customArtifactStream.setTemplateVariables(template.getVariables());
      return customArtifactStream;
    }
    return null;
  }

  private void updateEntity(ArtifactStream fromTemplate, ArtifactStream artifactStream) {
    artifactStream.setTemplateVariables(
        templateHelper.overrideVariables(fromTemplate.getTemplateVariables(), artifactStream.getTemplateVariables()));
    //    artifactStream.setTemplateVersion(fromTemplate.getTemplateVersion());
    if (fromTemplate instanceof CustomArtifactStream) {
      ((CustomArtifactStream) artifactStream).setScripts(((CustomArtifactStream) fromTemplate).getScripts());
    }
  }

  @Override
  public List<String> fetchTemplateProperties() {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public boolean checkTemplateDetailsChanged(BaseTemplate oldTemplate, BaseTemplate newTemplate) {
    DiffNode templateDetailsDiff = ObjectDifferBuilder.buildDefault().compare(newTemplate, oldTemplate);
    return templateDetailsDiff.hasChanges();
  }
}
