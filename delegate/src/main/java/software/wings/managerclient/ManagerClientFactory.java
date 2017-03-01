package software.wings.managerclient;

import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.http.ExponentialBackOff;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class ManagerClientFactory implements Provider<ManagerClient> {
  public final static TrustManager[] TRUST_ALL_CERTS =
      new X509TrustManager[] {new X509TrustManager(){public java.security.cert.X509Certificate[] getAcceptedIssuers(){
          return new java.security.cert.X509Certificate[] {};
}

public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
}
}
;

private final Logger logger = LoggerFactory.getLogger("http");
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

    OkHttpClient okHttpClient =
        new Builder()
            .connectionPool(new ConnectionPool())
            .retryOnConnectionFailure(true)
            .addInterceptor(new DelegateAuthInterceptor(tokenGenerator))
            .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS[0])
            .addInterceptor(chain -> {
              Request request = chain.request();

              long t1 = System.nanoTime();
              logger.debug(
                  String.format("Sending request %s on %s%n%s", request.url(), chain.connection(), request.headers()));

              Response response = chain.proceed(request);

              long t2 = System.nanoTime();
              logger.debug(String.format("Received response for %s in %.1fms%n%s\n", response.request().url(),
                  (t2 - t1) / 1e6d, response.headers()));

              return response;

            })
            .addInterceptor(chain -> ExponentialBackOff.executeForEver(() -> chain.proceed(chain.request())))
            .hostnameVerifier((hostname, session) -> true)
            .build();

    return okHttpClient;
  } catch (Exception e) {
    throw new RuntimeException(e);
  }
}
}
