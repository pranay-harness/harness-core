package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.BOJANA;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.AwsConfig;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AwsConfigYamlHandler;

public class AwsConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private AwsConfigYamlHandler yamlHandler;

  public static final String SAMPLE_STRING = "sample-string";

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testToBeanBothAccessKeyAndAccessKeySecretId() {
    AwsConfig.Yaml yaml = AwsConfig.Yaml.builder().accessKey(SAMPLE_STRING).accessKeySecretId(SAMPLE_STRING).build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Cloud Providers/test-harness.yaml")
                        .build();
    ChangeContext<AwsConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.CLOUD_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    assertThatThrownBy(() -> yamlHandler.toBean(null, changeContext, null))
        .hasMessageContaining("Cannot set both value and secret reference for accessKey field");
  }
}
