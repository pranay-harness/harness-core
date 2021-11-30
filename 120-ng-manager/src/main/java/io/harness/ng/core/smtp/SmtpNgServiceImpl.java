package io.harness.ng.core.smtp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;

import io.harness.annotations.dev.OwnedBy;
import io.harness.managerclient.SafeHttpCall;
import io.harness.ng.core.dto.NgSmtpDTO;
import io.harness.ng.core.dto.ValidationResultDTO;
import io.harness.ng.core.mapper.NgSmtpDTOMapper;
import io.harness.ng.core.mapper.ValidationResultDTOMapper;
import io.harness.rest.RestResponse;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class SmtpNgServiceImpl implements SmtpNgService {
  private NgSMTPSettingsHttpClient ngSMTPSettingsHttpClient;
  @Inject private TimeLimiter timeLimiter;

  @Inject
  public SmtpNgServiceImpl(NgSMTPSettingsHttpClient ngSMTPSettingsHttpClient) {
    this.ngSMTPSettingsHttpClient = ngSMTPSettingsHttpClient;
  }

  @Override
  public NgSmtpDTO saveSmtpSettings(NgSmtpDTO variable) throws IOException {
    SettingAttribute settingAttribute = NgSmtpDTOMapper.getSettingAttributeFromNgSmtpDTO(variable);
    RestResponse<SettingAttribute> response = SafeHttpCall.execute(
        ngSMTPSettingsHttpClient.saveSmtpSettings(GLOBAL_APP_ID, variable.getAccountId(), settingAttribute));
    return NgSmtpDTOMapper.getDTOFromSettingAttribute(response.getResource());
  }

  @Override
  public ValidationResultDTO validateSmtpSettings(String name, String accountId) throws IOException {
    RestResponse<Boolean> response = SafeHttpCall.execute(
        ngSMTPSettingsHttpClient.validateSmtpSettings(name, accountId, GLOBAL_APP_ID, GLOBAL_ENV_ID));
    ValidationResultDTO resultDTO = ValidationResultDTO.builder().valid(response.getResource()).build();
    if (!response.getResource()) {
      resultDTO.setErrorMessage("There already exists a connector with this name. Please try a different one.");
    }
    return resultDTO;
  }

  @Override
  public NgSmtpDTO updateSmtpSettings(NgSmtpDTO variable) throws IOException {
    SettingAttribute settingAttribute = NgSmtpDTOMapper.getSettingAttributeFromNgSmtpDTO(variable);
    RestResponse<SettingAttribute> response = SafeHttpCall.execute(
        ngSMTPSettingsHttpClient.updateSmtpSettings(variable.getUuid(), GLOBAL_APP_ID, settingAttribute));
    return NgSmtpDTOMapper.getDTOFromSettingAttribute(response.getResource());
  }

  @Override
  public ValidationResultDTO validateConnectivitySmtpSettings(
      NgSmtpDTO variable, String to, String subject, String body) throws IOException {
    SettingAttribute settingAttribute = NgSmtpDTOMapper.getSettingAttributeFromNgSmtpDTO(variable);
    RestResponse<ValidationResult> response =
        SafeHttpCall.execute(ngSMTPSettingsHttpClient.validateConnectivitySmtpSettings(
            GLOBAL_APP_ID, variable.getAccountId(), to, subject, body, settingAttribute));
    return ValidationResultDTOMapper.getDTOFromValidationResult(response.getResource());
  }

  @Override
  public Boolean deleteSmtpSettings(String id) throws IOException {
    RestResponse<Boolean> response =
        SafeHttpCall.execute(ngSMTPSettingsHttpClient.deleteSmtpSettings(id, GLOBAL_APP_ID));
    return response.getResource();
  }

  @Override
  public NgSmtpDTO getSmtpSettings(String id) throws IOException {
    RestResponse<SettingAttribute> response =
        SafeHttpCall.execute(ngSMTPSettingsHttpClient.getSmtpSettings(id, GLOBAL_APP_ID));
    return NgSmtpDTOMapper.getDTOFromSettingAttribute(response.getResource());
  }
}
