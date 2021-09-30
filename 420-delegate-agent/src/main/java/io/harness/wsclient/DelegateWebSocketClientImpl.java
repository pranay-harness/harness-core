package io.harness.wsclient;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.network.Http;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.DigestAuthentication;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

@Builder
@Slf4j
public class DelegateWebSocketClientImpl implements DelegateWebSocketClient {
  private static final String HTTPS = "HTTPS";
  private static final String SSL = "SSL";
  private String url;
  private Optional<Session> session;
  private Object webSocket;
  private Map<String, String> headers;

  @Inject
  public DelegateWebSocketClientImpl() {}

  public DelegateWebSocketClientImpl(
      String url, Optional<Session> session, Object webSocket, Map<String, String> headers) {
    this.url = url;
    this.session = session;
    this.webSocket = webSocket;
    this.headers = headers;
  }

  public boolean fire(String message) throws IOException {
    synchronized (this) {
      if (isOpen()) {
        session.get().getRemote().sendString(message);
        return true;
      }
      return false;
    }
  }

  public boolean isOpen() {
    try {
      return session.isPresent() && session.get().isOpen();
    } catch (Exception e) {
      log.error("Error checking web socket is open", e);
      return false;
    }
  }

  public void close() throws IOException {
    if (isOpen()) {
      session.get().disconnect();
    }
  }

  public boolean open(URI uri) {
    try {
      url = uri.toString();
      session = connect(url);
      return isOpen();
    } catch (Exception e) {
      log.error("Open WebSocket connection failed", e);
      return false;
    }
  }

  private Optional<Session> connect(String url) {
    try {
      HttpClient httpClient = configureHttpClient(url);
      httpClient.start();
      WebSocketClient client = new WebSocketClient(httpClient);
      client.start();
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      addHeadersToRequest(request);
      Future<Session> future = client.connect(webSocket, URI.create(url), request);
      return Optional.ofNullable(future.get(30L, TimeUnit.SECONDS));
    } catch (Exception e) {
      log.error("Error while establishing web socket connection: " + e);
      return Optional.empty();
    }
  }

  public void addHeader(String headerName, String headerValue) {
    if (headers == null) {
      headers = new HashMap<String, String>();
    }
    headers.put(headerName, headerValue);
  }

  @Override
  public void setWebSocket(Object o) {
    this.webSocket = o;
  }

  private void addHeadersToRequest(ClientUpgradeRequest request) {
    if (headers != null) {
      headers.forEach((i, j) -> request.setHeader(i, j));
    }
  }

  private HttpClient configureHttpClient(String url)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, URISyntaxException {
    KeyStore keyStore = getKeyStore();

    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    sslContextFactory.setEndpointIdentificationAlgorithm(HTTPS);
    sslContextFactory.setTrustStore(keyStore);
    sslContextFactory.setTrustManagerFactoryAlgorithm(TrustManagerFactory.getDefaultAlgorithm());

    HttpClient httpClient = new HttpClient(sslContextFactory);
    HttpHost proxyHost = Http.getHttpProxyHost(url);
    if (null != proxyHost && !isBlank(proxyHost.getHostName())) {
      Origin.Address address = new Origin.Address(proxyHost.getHostName(), proxyHost.getPort());
      HttpProxy proxy;
      if (!isBlank(Http.getProxyScheme()) && Http.getProxyScheme().equalsIgnoreCase(HTTPS)) {
        proxy = new HttpProxy(address, sslContextFactory);
      } else {
        proxy = new HttpProxy(address, false);
      }
      httpClient.getProxyConfiguration().getProxies().add(proxy);

      if (!isBlank(Http.getProxyUserName())) {
        AuthenticationStore authStore = httpClient.getAuthenticationStore();
        final String anyRealm = "<<ANY_REALM>>";
        URI uri = proxy.getURI();
        Authentication authentication =
            new DigestAuthentication(uri, anyRealm, Http.getProxyUserName(), Http.getProxyPassword());
        authStore.addAuthentication(authentication);
        authStore.addAuthenticationResult(
            new BasicAuthentication.BasicResult(uri, Http.getProxyUserName(), Http.getProxyPassword()));
        authStore.addAuthenticationResult(new BasicAuthentication.BasicResult(
            uri, HttpHeader.PROXY_AUTHORIZATION, Http.getProxyUserName(), Http.getProxyPassword()));
      }
    }
    return httpClient;
  }

  private KeyStore getKeyStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);

    // Load self-signed certificate created only for the purpose of local development
    try (InputStream certInputStream = getClass().getClassLoader().getResourceAsStream("localhost.pem")) {
      keyStore.setCertificateEntry(
          "localhost", (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(certInputStream));
    }

    // Load all trusted issuers from default java trust store
    TrustManagerFactory defaultTrustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    defaultTrustManagerFactory.init((KeyStore) null);
    for (TrustManager trustManager : defaultTrustManagerFactory.getTrustManagers()) {
      if (trustManager instanceof X509TrustManager) {
        for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
          keyStore.setCertificateEntry(acceptedIssuer.getSubjectDN().getName(), acceptedIssuer);
        }
      }
    }

    return keyStore;
  }
}
