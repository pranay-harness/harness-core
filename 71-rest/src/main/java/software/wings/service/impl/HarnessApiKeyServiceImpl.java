package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.HarnessApiKey;
import software.wings.beans.HarnessApiKey.AuthType;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.dl.WingsPersistence;
import software.wings.security.AuthenticationFilter;
import software.wings.security.annotations.HarnessApiKeyAuth;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Validator;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;

@Singleton
@ValidateOnExecution
public class HarnessApiKeyServiceImpl implements HarnessApiKeyService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public String generate(String clientType) {
    if (!ClientType.isValid(clientType)) {
      throw new WingsException("Invalid client type : " + clientType);
    }

    String apiKey;
    ClientType clientTypeObject = ClientType.valueOf(clientType);
    HarnessApiKey apiKeyObject = getApiKeyObject(clientTypeObject);
    if (apiKeyObject != null) {
      throw new WingsException("Api key already exists for the given client type.", USER);
    } else {
      apiKey = getNewKey();
      wingsPersistence.save(HarnessApiKey.builder()
                                .encryptedKey(EncryptionUtils.encrypt(apiKey.getBytes(Charset.forName("UTF-8")), null))
                                .clientType(clientTypeObject)
                                .build());
    }
    return apiKey;
  }

  private String extractToken(ContainerRequestContext requestContext, String prefix) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      throw new WingsException(INVALID_TOKEN, USER_ADMIN);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  private String getNewKey() {
    int KEY_LEN = 80;
    return CryptoUtil.secureRandAlphaNumString(KEY_LEN);
  }

  @Override
  public String get(String clientType) {
    if (!ClientType.isValid(clientType)) {
      throw new WingsException("Invalid client type : " + clientType);
    }

    HarnessApiKey globalApiKey =
        wingsPersistence.createQuery(HarnessApiKey.class).filter("clientType", clientType).get();
    Validator.notNullCheck("global api key null for client type " + clientType, globalApiKey, USER);
    return getDecryptedKey(globalApiKey.getEncryptedKey());
  }

  private String getDecryptedKey(byte[] encryptedKey) {
    return new String(EncryptionUtils.decrypt(encryptedKey, null), Charset.forName("UTF-8"));
  }

  private HarnessApiKey getApiKeyObject(ClientType clientType) {
    return wingsPersistence.createQuery(HarnessApiKey.class).filter("clientType", clientType).get();
  }

  @Override
  public boolean delete(String clientType) {
    if (!ClientType.isValid(clientType)) {
      throw new WingsException("Invalid client type : " + clientType);
    }

    HarnessApiKey apiKeyObject = getApiKeyObject(ClientType.valueOf(clientType));
    if (apiKeyObject != null) {
      wingsPersistence.delete(HarnessApiKey.class, apiKeyObject.getUuid());
      return true;
    }

    return false;
  }

  @Override
  public void validateHarnessClientApiRequest(ResourceInfo resourceInfo, ContainerRequestContext requestContext) {
    ClientType[] clientTypes = getClientTypesFromHarnessClientAuth(resourceInfo);

    if (isNotEmpty(clientTypes)) {
      boolean valid = Arrays.stream(clientTypes).anyMatch(clientType -> {
        if (clientType == null) {
          return false;
        }

        AuthType authType = clientType.getAuthType();
        String apiKeyFromHeader = null;

        if (AuthType.API_KEY_HEADER.equals(authType)) {
          apiKeyFromHeader = requestContext.getHeaderString(AuthenticationFilter.HARNESS_API_KEY_HEADER);
        } else if (AuthType.AUTH_HEADER.equals(authType)) {
          apiKeyFromHeader = extractToken(requestContext, "Bearer");
        }

        if (apiKeyFromHeader == null) {
          return false;
        }

        String apiKeyFromDB = get(clientType.name());
        if (isEmpty(apiKeyFromDB)) {
          return false;
        } else {
          if (apiKeyFromDB.equals(apiKeyFromHeader)) {
            return true;
          } else {
            return false;
          }
        }
      });

      if (!valid) {
        throw new WingsException(INVALID_TOKEN, USER);
      }
    } else {
      throw new InvalidRequestException("Invalid api annotation", USER);
    }
  }

  @Override
  public boolean validateHarnessClientApiRequest(ClientType clientType, String apiKey) {
    if (clientType == null) {
      return false;
    }

    if (apiKey == null) {
      return false;
    }

    String apiKeyFromDB = get(clientType.name());
    if (isEmpty(apiKeyFromDB)) {
      return false;
    } else {
      if (apiKeyFromDB.equals(apiKey)) {
        return true;
      } else {
        return false;
      }
    }
  }

  @Override
  public boolean isHarnessClientApi(ResourceInfo resourceInfo) {
    return resourceInfo.getResourceMethod().getAnnotation(HarnessApiKeyAuth.class) != null
        || resourceInfo.getResourceClass().getAnnotation(HarnessApiKeyAuth.class) != null;
  }

  private ClientType[] getClientTypesFromHarnessClientAuth(ResourceInfo resourceInfo) {
    Method resourceMethod = resourceInfo.getResourceMethod();
    HarnessApiKeyAuth methodAnnotations = resourceMethod.getAnnotation(HarnessApiKeyAuth.class);
    if (null != methodAnnotations) {
      return methodAnnotations.clientTypes();
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    HarnessApiKeyAuth classAnnotations = resourceClass.getAnnotation(HarnessApiKeyAuth.class);
    if (null != classAnnotations) {
      return classAnnotations.clientTypes();
    }

    return null;
  }
}