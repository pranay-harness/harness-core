package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretText;
import io.harness.rest.RestResponse;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.impl.security.auth.SettingAuthHandler;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.validation.SmtpConnectivityValidationAttributes;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Api(value = "/ng/settings", hidden = true)
@Path("/ng/settings")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
public class SettingResourceNg {
  @Inject private SettingAuthHandler settingAuthHandler;
  @Inject private SettingsService settingsService;
  @Inject private SettingServiceHelper settingServiceHelper;
  @Inject private SettingValidationService settingValidationService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private SecretManager secretManager;

  @POST
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<SettingAttribute> save(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, SettingAttribute variable) {
    SmtpConfig smtpConfig = (SmtpConfig) variable.getValue();
    SecretManagerConfig secretManagerConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
    SecretText secretText = new SecretText();
    String password = new String(smtpConfig.getPassword());
    secretText.setValue(password);
    String secretName = variable.getName() + "-" + accountId + "-SmtpSecret";
    secretText.setName(secretName);
    secretText.setScopedToAccount(true);
    secretText.setKmsId(secretManagerConfig.getUuid());
    smtpConfig.setPassword(secretManager.saveSecretText(accountId, secretText, true).toCharArray());
    variable.setValue(smtpConfig);
    settingAuthHandler.authorize(variable, appId);
    SettingAttribute savedSettingAttribute = settingsService.saveWithPruning(variable, appId, accountId);
    settingServiceHelper.updateSettingAttributeBeforeResponse(savedSettingAttribute, false);
    return new RestResponse<>(savedSettingAttribute);
  }

  @POST
  @Path("validateName")
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<Boolean> validate(@QueryParam("name") String name, @QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @DefaultValue(GLOBAL_ENV_ID) @QueryParam("envId") String envId) {
    return new RestResponse<>(settingValidationService.validateConnectorName(name, accountId, appId, envId));
  }

  @POST
  @Path("validate-connectivity")
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<ValidationResult> validateConnectivity(
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @QueryParam("to") String to, @QueryParam("subject") String subject, @QueryParam("body") String body,
      SettingAttribute variable) {
    variable.setCategory(SETTING);
    SmtpConnectivityValidationAttributes smtpAttr =
        SmtpConnectivityValidationAttributes.builder().to(to).body(body).subject(subject).build();
    variable.setValidationAttributes(smtpAttr);
    settingAuthHandler.authorize(variable, appId);
    return new RestResponse<>(settingsService.validateConnectivityWithPruning(variable, appId, accountId));
  }

  @GET
  @Path("{attrId}")
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<SettingAttribute> get(
      @PathParam("attrId") String attrId, @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId) {
    SettingAttribute result = settingsService.get(appId, attrId);
    settingServiceHelper.updateSettingAttributeBeforeResponse(result, true);
    secretManager.maskEncryptedFields((EncryptableSetting) result.getValue());
    return new RestResponse<>(result);
  }

  @PUT
  @Path("{attrId}")
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<SettingAttribute> update(@PathParam("attrId") String attrId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, SettingAttribute variable) {
    SettingAttribute existingAttribute = settingsService.get(appId, attrId);
    SmtpConfig existingSmtpConfig = (SmtpConfig) existingAttribute.getValue();
    SmtpConfig smtpConfig = (SmtpConfig) variable.getValue();
    SecretManagerConfig secretManagerConfig =
        secretManagerConfigService.getDefaultSecretManager(variable.getAccountId());
    SecretText secretText = new SecretText();
    String password = new String(smtpConfig.getPassword());
    secretText.setValue(password);
    String secretName = variable.getName() + "-" + variable.getAccountId() + "-SmtpSecret";
    secretText.setName(secretName);
    secretText.setScopedToAccount(true);
    secretText.setKmsId(secretManagerConfig.getUuid());
    String secretId = existingSmtpConfig.getEncryptedPassword();
    Boolean isSuccessful = secretManager.updateSecretText(variable.getAccountId(), secretId, secretText, true);
    smtpConfig.setPassword(secretId.toCharArray());
    variable.setValue(smtpConfig);
    settingAuthHandler.authorize(variable, appId);
    SettingAttribute updatedSettingAttribute = settingsService.updateWithSettingFields(variable, attrId, appId);
    settingServiceHelper.updateSettingAttributeBeforeResponse(updatedSettingAttribute, false);
    return new RestResponse<>(updatedSettingAttribute);
  }

  @DELETE
  @Path("{attrId}")
  @Timed
  @ExceptionMetered
  @InternalApi
  public RestResponse<Boolean> delete(
      @PathParam("attrId") String attrId, @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId) {
    SettingAttribute existingAttribute = settingsService.get(appId, attrId);
    SmtpConfig existingSmtpConfig = (SmtpConfig) existingAttribute.getValue();
    String storedSecretId = existingSmtpConfig.getEncryptedPassword();
    settingAuthHandler.authorize(appId, attrId);
    settingsService.delete(appId, attrId);
    secretManager.deleteSecret(existingAttribute.getAccountId(), storedSecretId, null, true);
    return new RestResponse<>(true);
  }
}
