package software.wings.yaml.gitSync;

import static software.wings.settings.SettingValue.SettingVariableTypes.YAML_GIT_SYNC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue.SettingVariableTypes;

import javax.validation.constraints.NotNull;

@Entity(value = "yamlGitConfig", noClassnameStored = true)
@Indexes(@Index(
    options = @IndexOptions(name = "locate", unique = true), fields = { @Field("accountId")
                                                                        , @Field("entityId") }))
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlGitConfig extends Base implements EncryptableSetting {
  @NotEmpty private String url;
  @NotEmpty private String branchName;
  private String username;

  @Encrypted @JsonView(JsonViews.Internal.class) private char[] password;
  private String sshSettingId;
  private boolean keyAuth;

  @SchemaIgnore @JsonIgnore private String encryptedPassword;

  @NotNull private SyncMode syncMode;
  private boolean enabled;
  private String webhookToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @Override
  public SettingVariableTypes getSettingType() {
    return YAML_GIT_SYNC;
  }

  public enum SyncMode { GIT_TO_HARNESS, HARNESS_TO_GIT, BOTH, NONE }

  public enum Type {
    SETUP,
    APP,
    SERVICE,
    SERVICE_COMMAND,
    ENVIRONMENT,
    SETTING,
    WORKFLOW,
    PIPELINE,
    TRIGGER,
    FOLDER,
    ARTIFACT_STREAM
  }

  @SchemaIgnore
  @JsonIgnore
  public GitConfig getGitConfig(SettingAttribute sshSettingAttribute) {
    return GitConfig.builder()
        .accountId(this.accountId)
        .repoUrl(this.url)
        .username(this.username)
        .password(this.password)
        .sshSettingAttribute(sshSettingAttribute)
        .sshSettingId(this.sshSettingId)
        .keyAuth(this.keyAuth)
        .encryptedPassword(this.encryptedPassword)
        .branch(this.branchName.trim())
        .build();
  }
}
