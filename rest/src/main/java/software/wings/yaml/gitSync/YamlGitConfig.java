package software.wings.yaml.gitSync;

import static software.wings.settings.SettingValue.SettingVariableTypes.YAML_GIT_SYNC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.beans.Base;
import software.wings.beans.GitConfig;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue.SettingVariableTypes;

import javax.validation.constraints.NotNull;

/**
 * Created by bsollish
 */
@Entity(value = "yamlGitConfig", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("entityId") }, options = @IndexOptions(unique = true)))
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlGitConfig extends Base implements Encryptable {
  @NotEmpty private String url;
  @NotEmpty private String branchName;
  @NotEmpty private String username;

  @NotNull @Encrypted @JsonView(JsonViews.Internal.class) private char[] password;

  @SchemaIgnore private String encryptedPassword;

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
  public GitConfig getGitConfig() {
    return GitConfig.builder()
        .accountId(this.accountId)
        .repoUrl(this.url)
        .username(this.username)
        .password(this.password)
        .encryptedPassword(this.encryptedPassword)
        .branch(this.branchName.trim())
        .build();
  }
}
