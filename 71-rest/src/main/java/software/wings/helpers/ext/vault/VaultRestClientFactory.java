package software.wings.helpers.ext.vault;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.network.Http;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.VaultConfig;
import software.wings.service.impl.security.VaultReadResponse;
import software.wings.service.impl.security.VaultReadResponseV2;
import software.wings.service.impl.security.VaultSecretValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A Factory class that's capable of constructing a REST client to talked to V1 or V2 secret engine backed
 * Vault server.
 * @author mark.lu on 10/11/18
 */
public class VaultRestClientFactory {
  private static final String PATH_SEPARATOR = "/";
  private static final String KEY_NAME_SEPARATOR = "#";

  private static final String DEFAULT_BASE_PATH = "harness";
  private static final String DEFAULT_KEY_NAME = "value";

  // This Jackson object mapper always ignore unknown properties while deserialize JSON documents.
  private static ObjectMapper objectMapper = new ObjectMapper();
  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static VaultRestClient create(final VaultConfig vaultConfig) {
    // http logging interceptor for dumping retrofit request/response content.
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    // set your desired log level, NONE by default. BODY while performing local testing
    logging.setLevel(Level.NONE);

    OkHttpClient httpClient = Http.getOkHttpClientWithNoProxyValueSet(vaultConfig.getVaultUrl())
                                  .readTimeout(10, TimeUnit.SECONDS)
                                  .addInterceptor(logging)
                                  .build();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(vaultConfig.getVaultUrl())
                                  .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                                  .client(httpClient)
                                  .build();

    int version = vaultConfig.getSecretEngineVersion();
    switch (version) {
      case 0:
      case 1:
        return new V1Impl(retrofit.create(VaultRestClientV1.class));
      case 2:
        return new V2Impl(retrofit.create(VaultRestClientV2.class));
      default:
        throw new IllegalArgumentException("Unsupported Vault secret engine version: " + version);
    }
  }

  public static String getFullPath(String basePath, String secretName, SettingVariableTypes settingVariableType) {
    return isEmpty(basePath) ? DEFAULT_BASE_PATH + PATH_SEPARATOR + settingVariableType + PATH_SEPARATOR + secretName
                             : basePath + PATH_SEPARATOR + settingVariableType + PATH_SEPARATOR + secretName;
  }

  /**
   * Note: the absolute path may look like something below:
   * /foo/bar/MySecret#MyKeyName
   */
  public static String getFullPath(String basePath, String secretPath) {
    boolean isAbsolutePath = secretPath.startsWith(PATH_SEPARATOR);
    if (isAbsolutePath) {
      // Stripping the leading '/' from the absolute path. Base path will be ignored.
      return secretPath.substring(1);
    } else {
      return isEmpty(basePath) ? DEFAULT_BASE_PATH + PATH_SEPARATOR + secretPath
                               : basePath + PATH_SEPARATOR + secretPath;
    }
  }

  private static VaultPathAndKey parseFullPath(String fullPath) {
    // Strip the leading '/' if present.
    fullPath = fullPath.startsWith(PATH_SEPARATOR) ? fullPath.substring(1) : fullPath;
    VaultPathAndKey result = new VaultPathAndKey();
    int index = fullPath.indexOf(KEY_NAME_SEPARATOR);
    if (index > 0) {
      result.path = fullPath.substring(0, index);
      result.keyName = fullPath.substring(index + 1);
    } else {
      result.path = fullPath;
      result.keyName = DEFAULT_KEY_NAME;
    }
    return result;
  }

  static class V1Impl implements VaultRestClient {
    private VaultRestClientV1 vaultRestClient;

    V1Impl(VaultRestClientV1 vaultRestClient) {
      this.vaultRestClient = vaultRestClient;
    }

    @Override
    public boolean writeSecret(String authToken, String fullPath, String value) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Map<String, String> valueMap = new HashMap<>();
      valueMap.put(pathAndKey.keyName, value);
      Response<Void> response = vaultRestClient.writeSecret(authToken, pathAndKey.path, valueMap).execute();
      return response.isSuccessful();
    }

    @Override
    public boolean deleteSecret(String authToken, String fullPath) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      return vaultRestClient.deleteSecret(authToken, pathAndKey.path).execute().isSuccessful();
    }

    @Override
    public String readSecret(String authToken, String fullPath) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);

      VaultReadResponse response = vaultRestClient.readSecret(authToken, pathAndKey.path).execute().body();
      return response == null || response.getData() == null ? null : response.getData().get(pathAndKey.keyName);
    }

    @Override
    public boolean renewToken(String authToken) throws IOException {
      return vaultRestClient.renewToken(authToken).execute().isSuccessful();
    }
  }

  static class V2Impl implements VaultRestClient {
    private VaultRestClientV2 vaultRestClient;

    V2Impl(VaultRestClientV2 vaultRestClient) {
      this.vaultRestClient = vaultRestClient;
    }

    @Override
    public boolean writeSecret(String authToken, String fullPath, String value) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Map<String, String> dataMap = new HashMap<>();
      dataMap.put(pathAndKey.keyName, value);
      VaultSecretValue vaultSecretValue = new VaultSecretValue(dataMap);
      Response<Void> response = vaultRestClient.writeSecret(authToken, pathAndKey.path, vaultSecretValue).execute();
      return response.isSuccessful();
    }

    @Override
    public boolean deleteSecret(String authToken, String fullPath) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      return vaultRestClient.deleteSecret(authToken, pathAndKey.path).execute().isSuccessful();
    }

    @Override
    public String readSecret(String authToken, String fullPath) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);

      VaultReadResponseV2 response = vaultRestClient.readSecret(authToken, pathAndKey.path).execute().body();
      return response == null || response.getData() == null ? null
                                                            : response.getData().getData().get(pathAndKey.keyName);
    }

    @Override
    public boolean renewToken(String authToken) throws IOException {
      return vaultRestClient.renewToken(authToken).execute().isSuccessful();
    }
  }

  private static class VaultPathAndKey {
    String path;
    String keyName;
  }
}