package software.wings.yaml.gitSync;

import static software.wings.settings.SettingVariableTypes.YAML_GIT_SYNC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingVariableTypes;

import javax.validation.constraints.NotNull;

@CdUniqueIndex(name = "locate", fields = { @Field("accountId")
                                           , @Field("entityId"), @Field("entityType") })
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "yamlGitConfig", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "YamlGitConfigKeys")
public class YamlGitConfig extends Base implements EncryptableSetting {
  public static final String ENTITY_ID_KEY = "entityId";
  public static final String ENTITY_TYPE_KEY = "entityType";
  public static final String WEBHOOK_TOKEN_KEY = "webhookToken";
  public static final String GIT_CONNECTOR_ID_KEY = "gitConnectorId";
  public static final String BRANCH_NAME_KEY = "branchName";
  public static final String SYNC_MODE_KEY = "syncMode";

  private String url;
  @NotEmpty private String branchName;
  private String username;

  @JsonView(JsonViews.Internal.class) private char[] password;
  private String sshSettingId;
  private boolean keyAuth;
  @NotEmpty private String gitConnectorId;

  @JsonIgnore @SchemaIgnore private String encryptedPassword;

  private SyncMode syncMode;
  private boolean enabled;
  private String webhookToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String entityId;
  @NotNull private EntityType entityType;
  @Transient private String entityName;

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

  @JsonIgnore
  @SchemaIgnore
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
