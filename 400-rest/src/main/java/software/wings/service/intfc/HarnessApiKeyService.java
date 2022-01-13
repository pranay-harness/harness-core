/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
