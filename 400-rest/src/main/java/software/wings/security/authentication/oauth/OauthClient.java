/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
