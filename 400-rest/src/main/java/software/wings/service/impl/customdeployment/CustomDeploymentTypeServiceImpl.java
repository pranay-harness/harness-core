/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.customdeployment;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.CustomDeploymentTypeNotFoundException;

import software.wings.beans.customdeployment.CustomDeploymentTypeDTO;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeAware;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;

@Slf4j
public class CustomDeploymentTypeServiceImpl implements CustomDeploymentTypeService {
  @Inject private TemplateService templateService;

  private static final BiFunction<Template, Boolean, CustomDeploymentTypeDTO> templateToDTOMapper =
      (template, withDetails) -> {
    if (template != null) {
      return CustomDeploymentTypeDTO.builder()
          .uuid(template.getUuid())
          .name(template.getName())
          .infraVariables(withDetails ? template.getVariables() : null)
          .build();
    }
    return null;
  };

  @Override
  public List<CustomDeploymentTypeDTO> list(@Nonnull String accountId, String appId, boolean withDetails) {
    final List<Template> customDeploymentTemplates =
        templateService.getTemplatesByType(accountId, appId, TemplateType.CUSTOM_DEPLOYMENT_TYPE);
    return emptyIfNull(customDeploymentTemplates)
        .stream()
        .map(t -> templateToDTOMapper.apply(t, withDetails))
        .collect(Collectors.toList());
  }

  @Override
  public CustomDeploymentTypeDTO get(@Nonnull String accountId, @Nonnull String templateId, @Nullable String version) {
    final Template deploymentTemplate;
    try {
      deploymentTemplate = templateService.get(templateId, version);
    } catch (Exception e) {
      throw new CustomDeploymentTypeNotFoundException("Deployment Type With Given Id Does Not Exist", e, USER);
    }
    return templateToDTOMapper.apply(deploymentTemplate, true);
  }

  @Override
  public CustomDeploymentTypeTemplate fetchDeploymentTemplate(
      String accountId, String templateId, @Nullable String version) {
    final Template template;
    try {
      template = templateService.get(accountId, templateId, version);
    } catch (Exception ex) {
      throw new CustomDeploymentTypeNotFoundException("Deployment Type With Given Id Does Not Exist", ex, USER);
    }
    return (CustomDeploymentTypeTemplate) template.getTemplateObject();
  }

  @Override
  public String fetchDeploymentTemplateUri(@NotBlank String templateUuid) {
    try {
      return templateService.makeNamespacedTemplareUri(templateUuid, null);
    } catch (Exception e) {
      throw new CustomDeploymentTypeNotFoundException("Cannot get deployment type", e, USER);
    }
  }

  @Override
  public String fetchDeploymentTemplateIdFromUri(@NotBlank String accountId, @NotBlank String templateUri) {
    try {
      return templateService.fetchTemplateIdFromUri(accountId, templateUri);
    } catch (Exception e) {
      throw new CustomDeploymentTypeNotFoundException("Cannot get deployment type", e, USER);
    }
  }

  private String fetchCustomDeploymentTypeName(String deploymentTypeTemplateId) {
    try {
      if (isNotEmpty(deploymentTypeTemplateId)) {
        final Template template = templateService.get(deploymentTypeTemplateId);
        return template.getName();
      }
    } catch (Exception e) {
      log.error("Linked Custom Deployment Type Not Found", e);
    }
    return null;
  }

  @Override
  public void putCustomDeploymentTypeNameIfApplicable(@Nonnull CustomDeploymentTypeAware entity) {
    if (entity != null) {
      entity.setDeploymentTypeName(fetchCustomDeploymentTypeName(entity.getDeploymentTypeTemplateId()));
    }
  }

  @Override
  public void putCustomDeploymentTypeNameIfApplicable(
      List<? extends CustomDeploymentTypeAware> entities, @NotBlank final String accountId) {
    if (isEmpty(entities) || isEmpty(accountId)) {
      return;
    }
    try {
      final List<Template> templates =
          templateService.batchGet(entities.stream()
                                       .filter(Objects::nonNull)
                                       .map(CustomDeploymentTypeAware::getDeploymentTypeTemplateId)
                                       .filter(EmptyPredicate::isNotEmpty)
                                       .distinct()
                                       .collect(Collectors.toList()),
              accountId);

      if (isNotEmpty(templates)) {
        final Map<String, String> templateIdToNameMap =
            templates.stream().collect(Collectors.toMap(Template::getUuid, Template::getName));
        entities.forEach(
            entity -> entity.setDeploymentTypeName(templateIdToNameMap.get(entity.getDeploymentTypeTemplateId())));
      }
    } catch (Exception ex) {
      log.error("Could not batch get deployment templates", ex);
    }
  }
}
