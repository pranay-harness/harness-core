package software.wings.helpers.ext.vault;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.network.Http;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.VaultConfig;
import software.wings.service.impl.security.VaultReadResponse;
import software.wings.service.impl.security.VaultReadResponseV2;
import software.wings.service.impl.security.VaultSecretValue;
import software.wings.service.impl.security.VaultSecretValueV2;
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

  static class V1Impl implements VaultRestClient {
    private VaultRestClientV1 vaultRestClient;

    V1Impl(VaultRestClientV1 vaultRestClient) {
      this.vaultRestClient = vaultRestClient;
    }

    @Override
    public boolean writeSecret(String authToken, String keyName, SettingVariableTypes settingType, String value)
        throws IOException {
      VaultSecretValue vaultSecretValue = new VaultSecretValue(value);
      return vaultRestClient.writeSecret(authToken, keyName, settingType, vaultSecretValue).execute().isSuccessful();
    }

    @Override
    public boolean deleteSecret(String authToken, String path) throws IOException {
      return vaultRestClient.deleteSecret(authToken, path).execute().isSuccessful();
    }

    @Override
    public String readSecret(String authToken, String keyName) throws IOException {
      VaultReadResponse response = vaultRestClient.readSecret(authToken, keyName).execute().body();
      return response.getData().getValue();
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
    public boolean writeSecret(String authToken, String keyName, SettingVariableTypes settingType, String value)
        throws IOException {
      Map<String, String> dataMap = new HashMap<>();
      dataMap.put("value", value);
      VaultSecretValueV2 vaultSecretValue = new VaultSecretValueV2(dataMap);
      return vaultRestClient.writeSecret(authToken, keyName, settingType, vaultSecretValue).execute().isSuccessful();
    }

    @Override
    public boolean deleteSecret(String authToken, String path) throws IOException {
      return vaultRestClient.deleteSecret(authToken, path).execute().isSuccessful();
    }

    @Override
    public String readSecret(String authToken, String keyName) throws IOException {
      VaultReadResponseV2 response = vaultRestClient.readSecret(authToken, keyName).execute().body();
      return response.getData().getData().get("value");
    }

    @Override
    public boolean renewToken(String authToken) throws IOException {
      return vaultRestClient.renewToken(authToken).execute().isSuccessful();
    }
  }
}
