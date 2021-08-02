package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptionType;

import software.wings.service.intfc.AppService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
@OwnedBy(CDP)
public class InfraProvisionStepYamlBuilder extends StepYamlBuilder {
  private static final String ENCRYPTED_TEXT = "ENCRYPTED_TEXT";
  private static final String VALUE_TYPE = "valueType";
  private static final String VALUE = "value";
  private static final List<String> ENCRYPTION_TYPES =
      Stream.of(EncryptionType.values()).map(EncryptionType::getYamlName).collect(Collectors.toList());
  public static final String YAML_REF_DELIMITER = ":";

  @Inject private AppService appService;
  @Inject private SecretManager secretManager;

  protected void convertPropertyIdsToNames(final String propertyName, final String appId, Object objectValue) {
    if (Objects.isNull(objectValue) || StringUtils.isBlank(appId)) {
      return;
    }

    String accountId = appService.getAccountIdByAppId(appId);
    if (StringUtils.isBlank(accountId)) {
      return;
    }

    try {
      BasicDBList subProperties = (BasicDBList) objectValue;
      subProperties.stream()
          .filter(Objects::nonNull)
          .map(BasicDBObject.class ::cast)
          .forEach(property -> replaceIdWithName(property, accountId));
    } catch (ClassCastException ex) {
      throw new InvalidArgumentsException(
          format("Unable to update cloud provider encrypted text values with ids for property: %s", propertyName), ex,
          USER);
    }
  }

  private void replaceIdWithName(BasicDBObject subProperty, final String accountId) {
    String valueType = String.valueOf(subProperty.get(VALUE_TYPE));
    if (ENCRYPTED_TEXT.equals(valueType)) {
      String secretYamlRef = (String) subProperty.get(VALUE);
      if (!isYamlRefSecretName(secretYamlRef)) {
        String encryptedYamlRef = secretManager.getEncryptedYamlRef(accountId, secretYamlRef);
        subProperty.put(VALUE, encryptedYamlRef);
      }
    }
  }

  protected void convertPropertyNamesToIds(final String propertyName, final String accountId, Object objectValue) {
    if (Objects.isNull(objectValue) || StringUtils.isBlank(accountId)) {
      return;
    }

    try {
      ArrayList<LinkedHashMap<String, String>> subProperties = (ArrayList<LinkedHashMap<String, String>>) objectValue;
      subProperties.stream().filter(Objects::nonNull).forEach(property -> replaceNameWithId(property, accountId));
    } catch (ClassCastException ex) {
      throw new InvalidArgumentsException(
          format("Unable to update cloud provider encrypted text values with names for property: %s", propertyName), ex,
          USER);
    } catch (SecretManagementException sme) {
      throw new InvalidRequestException(sme.getMessage(), USER);
    }
  }

  private void replaceNameWithId(LinkedHashMap<String, String> subProperty, final String accountId) {
    String valueType = subProperty.get(VALUE_TYPE);
    if (ENCRYPTED_TEXT.equals(valueType)) {
      String secretYamlRef = subProperty.get(VALUE);
      if (isYamlRefSecretName(secretYamlRef)) {
        EncryptedData encryptedYamlRef = secretManager.getEncryptedDataFromYamlRef(secretYamlRef, accountId);
        subProperty.put(VALUE, encryptedYamlRef.getUuid());
      }
    }
  }

  private boolean isYamlRefSecretName(final String secretYamlRef) {
    if (StringUtils.isBlank(secretYamlRef)) {
      return false;
    }

    String[] yamlRefs = secretYamlRef.split(YAML_REF_DELIMITER);
    return yamlRefs.length == 2 && ENCRYPTION_TYPES.contains(yamlRefs[0]);
  }
}
