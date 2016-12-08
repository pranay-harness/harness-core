package software.wings.managerclient;

import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

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

private String baseUrl;
private String accountId;
private String accountSecret;

public ManagerClientFactory(String baseUrl, String accountId, String accountSecret) {
  this.baseUrl = baseUrl;
  this.accountId = accountId;
  this.accountSecret = accountSecret;
}

@Override
public ManagerClient get() {
  Retrofit retrofit = new Retrofit.Builder()
                          .baseUrl(baseUrl)
                          .client(getUnsafeOkHttpClient())
                          .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper()))
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

    OkHttpClient okHttpClient = new Builder()
                                    .connectionPool(new ConnectionPool())
                                    .retryOnConnectionFailure(true)
                                    .addInterceptor(new DelegateAuthInterceptor(accountId, accountSecret))
                                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS[0])
                                    .hostnameVerifier((hostname, session) -> true)
                                    .build();

    return okHttpClient;
  } catch (Exception e) {
    throw new RuntimeException(e);
  }
}
}
