/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import io.harness.beans.ClientType;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

public interface HarnessApiKeyService {
  String PREFIX_BEARER = "Bearer";
  String PREFIX_API_KEY_TOKEN = "ApiKeyToken";

  String generate(String clientType);

  String get(String clientType);

  boolean delete(String clientType);

  void validateHarnessClientApiRequest(ResourceInfo resourceInfo, ContainerRequestContext requestContext);

  boolean validateHarnessClientApiRequest(ClientType clientType, String apiKey);

  boolean isHarnessClientApi(ResourceInfo resourceInfo);
}
