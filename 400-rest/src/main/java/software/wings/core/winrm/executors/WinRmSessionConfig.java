/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.core.winrm.executors;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.encryption.Encrypted;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@ToString(exclude = "password")
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class WinRmSessionConfig implements EncryptableSetting {
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotEmpty private String executionId;
  @NotEmpty private String commandUnitName;
  @NotEmpty private String hostname;
  private AuthenticationScheme authenticationScheme;
  private String domain;
  @NotEmpty private String username;
  @Encrypted(fieldName = "password") private String password;
  private boolean useKeyTab;
  private String keyTabFilePath;
  private int port;
  private boolean useSSL;
  private boolean skipCertChecks;
  private String workingDirectory;
  private final Map<String, String> environment;
  @Builder.Default private Integer timeout = (int) TimeUnit.MINUTES.toMillis(30);
  private boolean useNoProfile;

  @SchemaIgnore private String encryptedPassword;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.WINRM_SESSION_CONFIG;
  }

  @Override
  @JsonIgnore
  @SchemaIgnore
  public boolean isDecrypted() {
    return false;
  }

  @Override
  public void setDecrypted(boolean decrypted) {
    throw new IllegalStateException();
  }
}
