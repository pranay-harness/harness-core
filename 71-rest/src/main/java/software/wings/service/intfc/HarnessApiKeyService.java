package software.wings.service.intfc;

import software.wings.beans.HarnessApiKey.ClientType;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

public interface HarnessApiKeyService {
  String generate(String clientType);

  String get(String clientType);

  boolean delete(String clientType);

  void validateHarnessClientApiRequest(ResourceInfo resourceInfo, ContainerRequestContext requestContext);

  boolean validateHarnessClientApiRequest(ClientType clientType, String apiKey);

  boolean isHarnessClientApi(ResourceInfo resourceInfo);
}
