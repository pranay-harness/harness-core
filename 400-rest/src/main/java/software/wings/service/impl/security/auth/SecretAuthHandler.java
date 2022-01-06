/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(PL)
public class SecretAuthHandler {
  @Inject private AuthHandler authHandler;

  public void authorize() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_SECRETS));
    authHandler.authorizeAccountPermission(permissionAttributeList);
  }
}
