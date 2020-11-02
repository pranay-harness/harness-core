package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.service.impl.ldap.LdapHelper;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.function.Consumer;

/**
 * Class used for LDAP related validations.
 * Created by Pranjal on 08/21/2018
 */
@Slf4j
public class LDAPValidation extends AbstractSecretManagerValidation {
  @Inject private EncryptionService encryptionService;

  public LDAPValidation(final String delegateId, final DelegateTaskPackage delegateTaskPackage,
      final Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  private <T> T extractFirstParamenter(Object[] configList, Class<T> type) {
    for (Object config : configList) {
      if (type.isInstance(config)) {
        return (T) config;
      }
    }
    return null;
  }

  /**
   * Method to validate given configurations in parameters.
   * @return
   */
  @Override
  public List<DelegateConnectionResult> validate() {
    DelegateConnectionResult delegateConnectionResult = validateSecretManager();
    if (!delegateConnectionResult.isValidated()) {
      delegateConnectionResult.setCriteria(getCriteria().get(0));
      return singletonList(delegateConnectionResult);
    }

    boolean validated = false;
    Object[] configList = getParameters();
    LdapResponse response = null;
    LdapSettings settings = extractFirstParamenter(configList, LdapSettings.class);
    EncryptedDataDetail encryptedDataDetail = extractFirstParamenter(configList, EncryptedDataDetail.class);

    try {
      if (null != settings && null != encryptedDataDetail) {
        log.info("Validation config for delegate task validation is {}", settings.getConnectionSettings());
        settings.decryptFields(encryptedDataDetail, encryptionService);
        log.info("LTVF: Decryption of password was successful. Delegate task id {}", delegateTaskId);
        LdapConnectionSettings ldapConnectionConfig = settings.getConnectionSettings();
        LdapHelper ldapHelper = new LdapHelper(ldapConnectionConfig);
        response = ldapHelper.validateConnectionConfig();
      }

      if (response != null) {
        log.info("LTVF: Response status is: {}, task id is: {}", response.getStatus(), delegateTaskId);
      } else {
        log.info("LTVF: Response is null, task id is: {}", delegateTaskId);
      }

      if (response != null && response.getStatus() == Status.SUCCESS) {
        log.info("LTVF: Response status is: {}, task id is: {}", response.getStatus(), delegateTaskId);
        validated = true;
      }

      log.info("LTVF: Response received is: {}, task id is: {}", response, delegateTaskId);
      log.info("Validated LDAP delegate task {}, result is {}", delegateTaskId, validated);
    } catch (Exception e) {
      log.error("Failed to validate the ldap connection.", e);
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  /**
   * Gets the ldap server url.
   * @return
   */
  @Override
  public List<String> getCriteria() {
    return singletonList(((LdapSettings) getParameters()[2]).getUrl());
  }
}
