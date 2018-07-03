package software.wings.managerclient;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.network.FibonacciBackOff;
import io.harness.version.VersionInfoManager;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.security.TokenGenerator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */

class ManagerClientX509TrustManager implements X509TrustManager {
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    return new java.security.cert.X509Certificate[] {};
  }

  public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

  public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
}

public class ManagerClientFactory implements Provider<ManagerClient> {
  @SuppressFBWarnings("MS_MUTABLE_ARRAY")
  public static final TrustManager[] TRUST_ALL_CERTS = new X509TrustManager[] {new ManagerClientX509TrustManager()};

  @Inject private VersionInfoManager versionInfoManager;

  private static final Logger logger = LoggerFactory.getLogger("http");
  private String baseUrl;
  private TokenGenerator tokenGenerator;

  public ManagerClientFactory(String baseUrl, TokenGenerator tokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public ManagerClient get() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(getUnsafeOkHttpClient())
                            .addConverterFactory(new KryoConverterFactory())
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();
    return retrofit.create(ManagerClient.class);
  }

  private OkHttpClient getUnsafeOkHttpClient() {
    try {
      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      return new Builder()
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(new DelegateAuthInterceptor(tokenGenerator))
          .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS[0])
          .addInterceptor(chain
              -> chain.proceed(
                  chain.request()
                      .newBuilder()
                      .addHeader("User-Agent", "delegate/" + versionInfoManager.getVersionInfo().getVersion())
                      .addHeader("Version", versionInfoManager.getVersionInfo().getVersion())
                      .build()))
          .addInterceptor(chain -> FibonacciBackOff.executeForEver(() -> chain.proceed(chain.request())))
          .hostnameVerifier((hostname, session) -> true)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
