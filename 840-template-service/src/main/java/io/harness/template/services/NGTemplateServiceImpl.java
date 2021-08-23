package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.NGTemplateRepository;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.mappers.NGTemplateDtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@Slf4j
@OwnedBy(CDC)
public class NGTemplateServiceImpl implements NGTemplateService {
  @Inject private NGTemplateRepository templateRepository;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Template [%s] of label [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public TemplateEntity create(TemplateEntity templateEntity) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(
          templateEntity.getAccountId(), templateEntity.getIdentifier(), templateEntity.getVersionLabel());
      return templateRepository.save(templateEntity, NGTemplateDtoMapper.toDTO(templateEntity));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format(DUP_KEY_EXP_FORMAT_STRING, templateEntity.getIdentifier(), templateEntity.getVersionLabel(),
              templateEntity.getProjectIdentifier(), templateEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (Exception e) {
      log.error(String.format("Error while saving template [%s] of label [%s]", templateEntity.getIdentifier(),
                    templateEntity.getVersionLabel()),
          e);
      throw new InvalidRequestException(String.format("Error while saving template [%s] of label [%s]: %s",
          templateEntity.getIdentifier(), templateEntity.getVersionLabel(), e.getMessage()));
    }
  }
}
