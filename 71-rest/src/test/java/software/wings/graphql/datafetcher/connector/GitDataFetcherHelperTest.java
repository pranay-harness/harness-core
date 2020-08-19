package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.UrlType;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.mutation.connector.input.QLCustomCommitDetailsInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput.QLGitConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateGitConnectorInput.QLUpdateGitConnectorInputBuilder;

public class GitDataFetcherHelperTest {
  private static final String NAME = "NAME";
  private static final String URL = "URL";
  private static final String USER = "USER";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String BRANCH = "BRANCH";
  private static final String PASSWORD = "PASSWORD";
  private static final String AUTHOR_NAME = "AUTHOR_NAME";
  private static final String EMAIL = "EMAIL";
  private static final String MESSAGE = "MESSAGE";

  private GitDataFetcherHelper helper = new GitDataFetcherHelper();

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLGitConnectorInput input = getQlGitConnectorInputBuilder().build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(GitConfig.class);
    GitConfig config = (GitConfig) setting.getValue();
    assertThat(config.getRepoUrl()).isEqualTo(URL);
    assertThat(config.getUsername()).isEqualTo(USER);
    assertThat(config.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(config.getBranch()).isEqualTo(BRANCH);
    assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
    assertThat(config.isKeyAuth()).isEqualTo(Boolean.FALSE);
    assertThat(config.getSshSettingId()).isNull();
    assertThat(config.isGenerateWebhookUrl()).isEqualTo(Boolean.TRUE);
    assertThat(config.getAuthorName()).isEqualTo(AUTHOR_NAME);
    assertThat(config.getAuthorEmailId()).isEqualTo(EMAIL);
    assertThat(config.getCommitMessage()).isEqualTo(MESSAGE);
    assertThat(config.getUrlType()).isEqualTo(UrlType.REPO);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void toSettingAttributeWithSshKeyId() {
    QLGitConnectorInput input = getQlGitConnectorInputBuilder()
                                    .passwordSecretId(RequestField.absent())
                                    .sshSettingId(RequestField.ofNullable("SSH"))
                                    .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(GitConfig.class);
    GitConfig config = (GitConfig) setting.getValue();

    assertThat(config.isKeyAuth()).isEqualTo(Boolean.TRUE);
    assertThat(config.getSshSettingId()).isEqualTo("SSH");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateSettingAttributeReturnValue() {
    QLUpdateGitConnectorInput input = getQlUpdateGitConnectorInputBuilder().build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute().withValue(new GitConfig()).build();

    helper.updateSettingAttribute(settingAttribute, input);

    assertThat(settingAttribute).isNotNull();
    assertThat(settingAttribute.getName()).isEqualTo(NAME);
    assertThat(settingAttribute.getValue()).isInstanceOf(GitConfig.class);
    GitConfig config = (GitConfig) settingAttribute.getValue();
    assertThat(config.getRepoUrl()).isEqualTo(URL);
    assertThat(config.getUsername()).isEqualTo(USER);
    assertThat(config.getBranch()).isEqualTo(BRANCH);
    assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
    assertThat(config.isKeyAuth()).isEqualTo(Boolean.FALSE);
    assertThat(config.getSshSettingId()).isNull();
    assertThat(config.isGenerateWebhookUrl()).isEqualTo(Boolean.TRUE);
    assertThat(config.getAuthorName()).isEqualTo(AUTHOR_NAME);
    assertThat(config.getAuthorEmailId()).isEqualTo(EMAIL);
    assertThat(config.getCommitMessage()).isEqualTo(MESSAGE);
    assertThat(config.getAuthenticationScheme()).isEqualTo(HTTP_PASSWORD);
    assertThat(config.getUrlType()).isEqualTo(UrlType.REPO);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithSshKeyId() {
    QLUpdateGitConnectorInput input = getQlUpdateGitConnectorInputBuilder()
                                          .passwordSecretId(RequestField.absent())
                                          .sshSettingId(RequestField.ofNullable("SSH"))
                                          .build();
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withValue(GitConfig.builder().build()).build();

    helper.updateSettingAttribute(settingAttribute, input);

    assertThat(settingAttribute).isNotNull();
    assertThat(settingAttribute.getName()).isEqualTo(NAME);
    assertThat(settingAttribute.getValue()).isInstanceOf(GitConfig.class);
    GitConfig config = (GitConfig) settingAttribute.getValue();
    assertThat(config.isKeyAuth()).isEqualTo(Boolean.TRUE);
    assertThat(config.getSshSettingId()).isEqualTo("SSH");
  }

  private QLUpdateGitConnectorInputBuilder getQlUpdateGitConnectorInputBuilder() {
    QLCustomCommitDetailsInput customCommitDetailsInput = QLCustomCommitDetailsInput.builder()
                                                              .authorName(RequestField.ofNullable(AUTHOR_NAME))
                                                              .authorEmailId(RequestField.ofNullable(EMAIL))
                                                              .commitMessage(RequestField.ofNullable(MESSAGE))
                                                              .build();
    return QLUpdateGitConnectorInput.builder()
        .name(RequestField.ofNullable(NAME))
        .URL(RequestField.ofNullable(URL))
        .userName(RequestField.ofNullable(USER))
        .branch(RequestField.ofNullable(BRANCH))
        .passwordSecretId(RequestField.ofNullable(PASSWORD))
        .sshSettingId(RequestField.absent())
        .generateWebhookUrl(RequestField.ofNullable(Boolean.TRUE))
        .customCommitDetails(RequestField.ofNullable(customCommitDetailsInput));
  }

  private QLGitConnectorInputBuilder getQlGitConnectorInputBuilder() {
    QLCustomCommitDetailsInput customCommitDetailsInput = QLCustomCommitDetailsInput.builder()
                                                              .authorName(RequestField.ofNullable(AUTHOR_NAME))
                                                              .authorEmailId(RequestField.ofNullable(EMAIL))
                                                              .commitMessage(RequestField.ofNullable(MESSAGE))
                                                              .build();
    return QLGitConnectorInput.builder()
        .name(RequestField.ofNullable(NAME))
        .URL(RequestField.ofNullable(URL))
        .urlType(RequestField.ofNullable(UrlType.REPO))
        .userName(RequestField.ofNullable(USER))
        .branch(RequestField.ofNullable(BRANCH))
        .passwordSecretId(RequestField.ofNullable(PASSWORD))
        .sshSettingId(RequestField.absent())
        .generateWebhookUrl(RequestField.ofNullable(Boolean.TRUE))
        .customCommitDetails(RequestField.ofNullable(customCommitDetailsInput));
  }
}
