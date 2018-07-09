package software.wings.helpers.ext.jenkins;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.offbytwo.jenkins.client.JenkinsHttpClient;
import com.offbytwo.jenkins.client.PreemptiveAuth;
import io.harness.network.Http;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.protocol.BasicHttpContext;

import java.net.URI;

/**
 * Created by sgurubelli on 8/14/17.
 * To accept Untrusted certificates from delegate
 */
public class CustomJenkinsHttpClient extends JenkinsHttpClient {
  public CustomJenkinsHttpClient(URI uri, HttpClientBuilder builder) {
    super(uri, builder);
  }

  public CustomJenkinsHttpClient(URI uri, String username, String password, HttpClientBuilder builder) {
    super(uri, addAuthentication(builder, uri, username, password));
    if (isNotEmpty(username)) {
      BasicHttpContext basicHttpContext = new BasicHttpContext();
      basicHttpContext.setAttribute("preemptive-auth", new BasicScheme());
      setLocalContext(basicHttpContext);
    }
  }

  public CustomJenkinsHttpClient(URI uri, String token, HttpClientBuilder builder) {
    super(uri, builder.addInterceptorFirst((HttpRequestInterceptor) (httpRequest, httpContext) -> {
      httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }));
  }

  public static HttpClientBuilder addAuthentication(
      HttpClientBuilder builder, URI uri, String username, String password) {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();

    setProxyAuthForClient(builder, credsProvider);
    if (isNotEmpty(username)) {
      AuthScope scope = new AuthScope(uri.getHost(), uri.getPort());
      UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
      credsProvider.setCredentials(scope, credentials);
    }

    builder.setDefaultCredentialsProvider(credsProvider);
    builder.addInterceptorFirst(new PreemptiveAuth());
    return builder;
  }

  private static void setProxyAuthForClient(HttpClientBuilder builder, CredentialsProvider credsProvider) {
    if (Http.getHttpProxyHost() != null) {
      builder.setProxy(new HttpHost(Http.getProxyHostName(), Integer.parseInt(Http.getProxyPort())));
      if (isNotEmpty(Http.getProxyUserName())) {
        builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
        credsProvider.setCredentials(new AuthScope(Http.getProxyHostName(), Integer.parseInt(Http.getProxyPort())),
            new UsernamePasswordCredentials(Http.getProxyUserName(), Http.getProxyPassword()));
      }
    }
  }
}
