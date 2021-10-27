package io.harness.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ConfigSecretReflectionUtils {
  public SecretManagerServiceClient client;
  public void resolveSecret(Object o, String secretManagerProject) throws IOException {
    Class<?> c = o.getClass();
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        ConfigSecret annotation = f.getAnnotation(ConfigSecret.class);
        if (annotation != null) {
          boolean isAccessible = f.isAccessible();
          f.setAccessible(true);
          try {
            Object object = f.get(o);
            if (object != null) {
              if (object instanceof String && isNotEmpty(object.toString())) {
                String value = fetchSecret(secretManagerProject, object.toString());
                f.set(o, value);
              } else if (object instanceof char[] && ((char[]) object).length > 0) {
                String value = fetchSecret(secretManagerProject, String.copyValueOf((char[]) object));
                f.set(o, value.toCharArray());
              } else {
                resolveSecret(object, secretManagerProject);
              }
            }
            f.setAccessible(isAccessible);
          } catch (IllegalAccessException e) {
            log.error("Field [{}] is not accessible ", f.getName());
          }
          f.setAccessible(isAccessible);
        }
      }
      c = c.getSuperclass();
    }
  }

  private String fetchSecret(String project, String secretId) throws IOException {
    try {
      SecretVersionName secretVersionName = SecretVersionName.of(project, secretId, "latest");
      AccessSecretVersionResponse response = getSecretManager().accessSecretVersion(secretVersionName);
      return response.getPayload().getData().toStringUtf8();
    } catch (Exception e) {
      log.warn(String.format("Secret: %s not found in %s : %s", secretId, project, e.getMessage()));
      throw e;
    }
  }

  private SecretManagerServiceClient getSecretManager() throws IOException {
    if (ConfigSecretReflectionUtils.client == null) {
      ConfigSecretReflectionUtils.client = SecretManagerServiceClient.create();
    }
    return ConfigSecretReflectionUtils.client;
  }
}
