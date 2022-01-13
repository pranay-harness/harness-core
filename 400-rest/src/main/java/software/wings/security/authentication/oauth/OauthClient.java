/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

@OwnedBy(PL)
public interface OauthClient {
  String getName();

  URI getRedirectUrl();

  OauthUserInfo execute(String code, String state) throws InterruptedException, ExecutionException, IOException;
}
