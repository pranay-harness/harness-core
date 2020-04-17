package software.wings.beans;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.settings.SettingValue;

import java.util.List;

public class SettingAttributeTest extends CategoryTest {
  private static final String PASSWORD = "password";
  private static final String TOKEN = "token";

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForNullValue() {
    List<String> secretIds = prepareSettingAttribute(null).fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForGCSHelm() {
    List<String> secretIds = prepareSettingAttribute(GCSHelmRepoConfig.builder().build()).fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForDocker() {
    List<String> secretIds = prepareSettingAttribute(
        DockerConfig.builder().dockerRegistryUrl("docker.registry").encryptedPassword(PASSWORD).build())
                                 .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(PASSWORD);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForJenkins() {
    List<String> secretIds = prepareSettingAttribute(JenkinsConfig.builder()
                                                         .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                                                         .encryptedPassword(PASSWORD)
                                                         .encryptedToken(TOKEN)
                                                         .build())
                                 .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(PASSWORD);

    secretIds = prepareSettingAttribute(JenkinsConfig.builder()
                                            .authMechanism(JenkinsUtils.TOKEN_FIELD)
                                            .encryptedPassword(PASSWORD)
                                            .encryptedToken(TOKEN)
                                            .build())
                    .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(TOKEN);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForGit() {
    List<String> secretIds =
        prepareSettingAttribute(GitConfig.builder().keyAuth(false).encryptedPassword(PASSWORD).build())
            .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(PASSWORD);

    secretIds = prepareSettingAttribute(GitConfig.builder().keyAuth(true).encryptedPassword(PASSWORD).build())
                    .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).isEmpty();
  }

  private SettingAttribute prepareSettingAttribute(SettingValue value) {
    return aSettingAttribute().withAccountId(ACCOUNT_ID).withUuid(SETTING_ID).withValue(value).build();
  }
}
