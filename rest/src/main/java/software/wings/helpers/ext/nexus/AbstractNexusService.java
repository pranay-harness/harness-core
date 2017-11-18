package software.wings.helpers.ext.nexus;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.HttpUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@Singleton
public abstract class AbstractNexusService implements NexusService {
  @Inject private EncryptionService encryptionService;
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected String getBaseUrl(NexusConfig nexusConfig) {
    String baseUrl = nexusConfig.getNexusUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    return baseUrl;
  }

  private Retrofit getRetrofit(String baseUrl, Converter.Factory converterFactory) {
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(HttpUtil.getUnsafeOkHttpClient())
        .build();
  }

  protected NexusRestClient getRestClient(final NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(nexusConfig, encryptionDetails);
    return getRetrofit(getBaseUrl(nexusConfig), SimpleXmlConverterFactory.createNonStrict())
        .create(NexusRestClient.class);
  }

  protected NexusThreeRestClient getNexusThreeClient(
      final NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(nexusConfig, encryptionDetails);
    return getRetrofit(getBaseUrl(nexusConfig), JacksonConverterFactory.create()).create(NexusThreeRestClient.class);
  }

  protected boolean isSuccessful(Response<?> response) throws IOException {
    if (response != null && !response.isSuccessful()) {
      logger.error("Request not successful. Reason: {}", response);
      // TODO : Proper Error handling --> Get the code and map to Wings Error code
      int code = response.code();
      ErrorCode errorCode = ErrorCode.DEFAULT_ERROR_CODE;

      switch (code) {
        case 404:
          return false;
        case 401:
          throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Nexus credentials");
      }
      throw new WingsException(errorCode, "message", response.message());
    }
    return true;
  }
}
