package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.DHRUV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.sourcerepoprovider.GitConfigYamlHandler;
import software.wings.settings.SettingValue;

public class GitConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private GitConfigYamlHandler yamlHandler;
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";
  public static final String SAMPLE_STRING = "sample-string";
  private Class yamlClass = GitConfig.Yaml.class;

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testToBean() {
    GitConfig.Yaml yaml = GitConfig.Yaml.builder()
                              .branch(SAMPLE_STRING)
                              .reference(SAMPLE_STRING)
                              .keyAuth(true)
                              .sshKeyName(null)
                              .authorName(SAMPLE_STRING)
                              .authorEmailId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .password(SAMPLE_STRING)
                              .username(SAMPLE_STRING)
                              .url(SAMPLE_STRING)
                              .build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Source Repo Provider/test-harness.yaml")
                        .build();
    ChangeContext<GitConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.SOURCE_REPO_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    SettingAttribute settingAttribute = yamlHandler.toBean(null, changeContext, null);
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getCommitMessage()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getAuthorEmailId()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getAuthorName()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getSshSettingId()).isEqualTo(null);
    assertThat(gitConfig.getUsername()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getBranch()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getRepoUrl()).isEqualTo(SAMPLE_STRING);
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testToYaml() {
    GitConfig gitConfig = GitConfig.builder()
                              .authorEmailId(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .branch(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .sshSettingId(null)
                              .build();

    SettingValue settingValue = (SettingValue) gitConfig;
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(SAMPLE_STRING)
                                            .withUuid(null)
                                            .withValue(settingValue)
                                            .build();

    GitConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, null);

    assertThat(yaml).isNotNull();
    assertThat(yaml.getCommitMessage()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getAuthorEmailId()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getAuthorName()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getBranch()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getDescription()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getSshKeyName()).isEqualTo(null);
  }
}
