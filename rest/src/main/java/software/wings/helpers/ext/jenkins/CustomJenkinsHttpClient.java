package software.wings.helpers.ext.jenkins;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.offbytwo.jenkins.client.JenkinsHttpClient;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
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
    if (isNotBlank(username)) {
      BasicHttpContext basicHttpContext = new BasicHttpContext();
      basicHttpContext.setAttribute("preemptive-auth", new BasicScheme());
      setLocalContext(basicHttpContext);
    }
  }
}
