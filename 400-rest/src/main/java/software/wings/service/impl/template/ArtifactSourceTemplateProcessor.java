/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.template.TemplateType.ARTIFACT_SOURCE;
import static software.wings.common.TemplateConstants.LATEST_TAG;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;

import software.wings.beans.EntityType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class ArtifactSourceTemplateProcessor extends AbstractTemplateProcessor {
  @Inject ArtifactStreamService artifactStreamService;
  @Inject TemplateHelper templateHelper;

  @Override
  public TemplateType getTemplateType() {
    return ARTIFACT_SOURCE;
  }

  @Override
  public Template process(Template template) {
    template.setType(getTemplateType().name());
    Set<String> keywords = generateKeyword(template);
    if (EmptyPredicate.isNotEmpty(keywords)) {
      template.setKeywords(keywords);
    }

    ensureDefaults(template);
    return template;
  }

  private Set<String> generateKeyword(Template template) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
    if (artifactSourceTemplate.getArtifactSource() instanceof CustomArtifactSourceTemplate) {
      return new HashSet<>(singletonList(ArtifactStreamType.CUSTOM.name()));
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
      log.info("Template {} was deleted. Not updating linked entities", template.getUuid());
      return;
    }

    // Read all the service commands that references the given
    try (HIterator<ArtifactStream> iterator =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)
                                 .filter(ArtifactStreamKeys.templateUuid, template.getUuid())
                                 .fetch())) {
      for (ArtifactStream artifactStream : iterator) {
        try {
          String templateVersion = artifactStream.getTemplateVersion();
          if (templateVersion == null || templateVersion.equalsIgnoreCase(LATEST_TAG)) {
            log.info("Updating the linked artifact stream with id: {}", artifactStream.getUuid());
            ArtifactStream entityFromTemplate = constructEntityFromTemplate(template, EntityType.ARTIFACT_STREAM);
            if (entityFromTemplate != null) {
              updateEntity(entityFromTemplate, artifactStream);
              // for CUSTOM, we want to skip validation if the artifact stream update is a side effect of template
              // update. This is to avoid executing the custom scripts on delegate as part of update which can be very
              // time consuming causing some of the template library updates to not propagate to all the linked artifact
              // streams.
              artifactStreamService.update(artifactStream, false, true);
              log.info("Linked artifact stream with id: {} updated", artifactStream.getUuid());
            } else {
              log.warn("Failed to update the linked Artifact Stream with id: {}", artifactStream.getUuid());
            }
          } else {
            log.info("The template linked to Artifact Stream with id: {} is not the latest. So, not updating it",
                artifactStream.getUuid());
          }
        } catch (Exception e) {
          log.warn("Failed to update the linked Artifact Stream with id: {}", artifactStream.getUuid(), e);
        }
      }
    }
  }

  @Override
  public ArtifactStream constructEntityFromTemplate(Template template, EntityType entityType) {
    switch (entityType) {
      case ARTIFACT_STREAM:
        ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
        if (artifactSourceTemplate.getArtifactSource() instanceof CustomArtifactSourceTemplate) {
          CustomArtifactSourceTemplate customArtifactSourceTemplate =
              (CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource();
          CustomArtifactStream customArtifactStream =
              CustomArtifactStream.builder()
                  .scripts(
                      asList(CustomArtifactStream.Script.builder()
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
      default:
        throw new InvalidRequestException("Unsupported Entity Type");
    }
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
