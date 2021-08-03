package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.security.SecretManager;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(HarnessTeam.CDP)
public class TerraformProvisionStepYamlBuilderTest extends WingsBaseTest {
  private static final String NAME = "name";
  private static final String VALUE = "value";
  private static final String VALUE_TYPE = "valueType";
  private static final String VARIABLES_PROPERTY = "variables";
  private static String APP_ID = "app-id";
  private static String ACCOUNT_ID = "accountId";

  @Mock private AppService appService;
  @Mock private SecretManager secretManager;

  @InjectMocks private TerraformProvisionStepYamlBuilder cloudProvisionStepYamlBuilder;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testConvertIdToNameForKnownTypes() {
    String encryptedId = "encryptedId";
    String expectedSecretName = "secretKey:secretName";

    BasicDBList objectValue = populateVariablesPropertyBasicDBList(encryptedId);
    Map<String, Object> outputProperties = new HashMap<>();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(secretManager.getEncryptedYamlRef(ACCOUNT_ID, encryptedId)).thenReturn(expectedSecretName);

    cloudProvisionStepYamlBuilder.convertIdToNameForKnownTypes(
        VARIABLES_PROPERTY, objectValue, outputProperties, APP_ID, new HashMap<>());

    assertThat(outputProperties.size()).isEqualTo(1);
    assertThat(outputProperties.get(VARIABLES_PROPERTY)).isInstanceOf(BasicDBList.class);
    BasicDBList variables = (BasicDBList) outputProperties.get(VARIABLES_PROPERTY);
    assertThat(variables.size()).isEqualTo(2);
    assertThat(variables.get(0)).isInstanceOf(BasicDBObject.class);
    assertThat(variables).contains(getPlainTextBasicDBObject());
    BasicDBObject secretBasicDBObject = getSecretBasicDBObject(encryptedId);
    secretBasicDBObject.put(VALUE, expectedSecretName);
    assertThat(variables).contains(secretBasicDBObject);
  }

  private BasicDBList populateVariablesPropertyBasicDBList(String encryptedId) {
    BasicDBObject secretSubProperty = getSecretBasicDBObject(encryptedId);
    BasicDBObject plainSubProperty = getPlainTextBasicDBObject();

    BasicDBList basicDBList = new BasicDBList();
    basicDBList.add(plainSubProperty);
    basicDBList.add(secretSubProperty);

    return basicDBList;
  }

  private BasicDBObject getSecretBasicDBObject(String encryptedId) {
    BasicDBObject secretSubProperty = new BasicDBObject();
    secretSubProperty.put(NAME, "secretSubProperty");
    secretSubProperty.put(VALUE, encryptedId);
    secretSubProperty.put(VALUE_TYPE, "ENCRYPTED_TEXT");

    return secretSubProperty;
  }

  private BasicDBObject getPlainTextBasicDBObject() {
    BasicDBObject plainSubProperty = new BasicDBObject();
    plainSubProperty.put(NAME, "plainSubProperty");
    plainSubProperty.put(VALUE, "plainText");
    plainSubProperty.put(VALUE_TYPE, "TEXT");

    return plainSubProperty;
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testConvertNameToIdForKnownTypes() {
    String secretName = "safeharness:secretName";
    String expectedEncryptedId = "encryptedId";

    List<LinkedHashMap<String, String>> objectValue = populateVariablesPropertyList(secretName);
    Map<String, Object> outputProperties = new HashMap<>();

    EncryptedData encryptedData = EncryptedData.builder().uuid(expectedEncryptedId).build();
    when(secretManager.getEncryptedDataFromYamlRef(secretName, ACCOUNT_ID)).thenReturn(encryptedData);

    cloudProvisionStepYamlBuilder.convertNameToIdForKnownTypes(
        VARIABLES_PROPERTY, objectValue, outputProperties, APP_ID, ACCOUNT_ID, new HashMap<>());

    assertThat(outputProperties.size()).isEqualTo(1);
    assertThat(outputProperties.get(VARIABLES_PROPERTY)).isInstanceOf(ArrayList.class);
    List<LinkedHashMap<String, String>> variables =
        (ArrayList<LinkedHashMap<String, String>>) outputProperties.get(VARIABLES_PROPERTY);
    assertThat(variables.size()).isEqualTo(2);
    assertThat(variables.get(0)).isInstanceOf(LinkedHashMap.class);
    assertThat(variables).contains(getPlainTextLinkedHashMap());
    LinkedHashMap<String, String> secretSubProperty = getSecretLinkedHashMap(secretName);
    secretSubProperty.put(VALUE, expectedEncryptedId);
    assertThat(variables).contains(secretSubProperty);
  }

  private List<LinkedHashMap<String, String>> populateVariablesPropertyList(String secretName) {
    LinkedHashMap<String, String> secretSubProperty = getSecretLinkedHashMap(secretName);
    LinkedHashMap<String, String> plainSubProperty = getPlainTextLinkedHashMap();

    List<LinkedHashMap<String, String>> subPropertiesList = new ArrayList<>();
    subPropertiesList.add(plainSubProperty);
    subPropertiesList.add(secretSubProperty);

    return subPropertiesList;
  }

  private LinkedHashMap<String, String> getSecretLinkedHashMap(String secretName) {
    LinkedHashMap<String, String> secretSubProperty = new LinkedHashMap<>();
    secretSubProperty.put(NAME, "secretSubProperty");
    secretSubProperty.put(VALUE, secretName);
    secretSubProperty.put(VALUE_TYPE, "ENCRYPTED_TEXT");

    return secretSubProperty;
  }

  private LinkedHashMap<String, String> getPlainTextLinkedHashMap() {
    LinkedHashMap<String, String> plainSubProperty = new LinkedHashMap<>();
    plainSubProperty.put(NAME, "plainSubProperty");
    plainSubProperty.put(VALUE, "plainText");
    plainSubProperty.put(VALUE_TYPE, "TEXT");

    return plainSubProperty;
  }
}
