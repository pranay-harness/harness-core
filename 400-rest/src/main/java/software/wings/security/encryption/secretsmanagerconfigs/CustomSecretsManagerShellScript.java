/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.annotations.dev.HarnessModule._440_SECRET_MANAGEMENT_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@TargetModule(_440_SECRET_MANAGEMENT_SERVICE)
public class CustomSecretsManagerShellScript {
  @NonNull private ScriptType scriptType;
  @NonNull private String scriptString;
  @NonNull private List<String> variables;
  private long timeoutMillis;

  public enum ScriptType { BASH, POWERSHELL }
}
