/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

/**
 * Class used to specify the user invitation source such as manual, ldap group sync, etc.
 *
 * @author Swapnil
 */

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class UserInviteSource {
  public enum SourceType { MANUAL, SSO, TRIAL, MARKETPLACE, MARKETO_LINKEDIN, AZURE_MARKETPLACE, ONPREM }

  @Default SourceType type = SourceType.MANUAL;
  @Default String uuid = StringUtils.EMPTY;
}
