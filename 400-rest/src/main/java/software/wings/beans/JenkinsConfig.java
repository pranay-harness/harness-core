package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.jersey.JsonViews;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@JsonTypeName("JENKINS")
@Data
@ToString(exclude = {"password", "token"})
@EqualsAndHashCode(callSuper = false)
public class JenkinsConfig extends SettingValue
    implements EncryptableSetting, ArtifactSourceable, TaskParameters, ExecutionCapabilityDemander {
  public static final String USERNAME_DEFAULT_TEXT = "UserName/Password";

  @Attributes(title = "Jenkins URL", required = true) @NotEmpty private String jenkinsUrl;
  @Attributes(title = "Use Connector URL for Job execution") private boolean useConnectorUrlForJobExecution;
  @Attributes(
      title = "Authentication Mechanism", required = true, enums = {USERNAME_DEFAULT_TEXT, JenkinsUtils.TOKEN_FIELD})
  @NotEmpty
  private String authMechanism;

  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password/ API Token") @Encrypted(fieldName = "password/api_token") private char[] password;
  @Attributes(title = "Bearer Token(HTTP Header)") @Encrypted(fieldName = "bearer_token") private char[] token;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedToken;

  /**
   * Instantiates a new jenkins config.
   */
  public JenkinsConfig() {
    super(SettingVariableTypes.JENKINS.name());
    authMechanism = USERNAME_DEFAULT_TEXT;
  }

  @Builder
  public JenkinsConfig(String jenkinsUrl, String username, char[] password, String accountId, String encryptedPassword,
      char[] token, String encryptedToken, String authMechanism, boolean useConnectorUrlForJobExecution) {
    super(SettingVariableTypes.JENKINS.name());
    this.jenkinsUrl = jenkinsUrl;
    this.useConnectorUrlForJobExecution = useConnectorUrlForJobExecution;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.authMechanism = authMechanism;
    this.encryptedToken = encryptedToken;
    this.token = token;
  }

  @Override
  public String fetchUserName() {
    return getUsername();
  }

  @Override
  public String fetchRegistryUrl() {
    return getJenkinsUrl();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        jenkinsUrl, maskingEvaluator));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<String> fetchRelevantEncryptedSecrets() {
    if (JenkinsUtils.TOKEN_FIELD.equals(authMechanism)) {
      return Collections.singletonList(encryptedToken);
    } else {
      return Collections.singletonList(encryptedPassword);
    }
  }
}
