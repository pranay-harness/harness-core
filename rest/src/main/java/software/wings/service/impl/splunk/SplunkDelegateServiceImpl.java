package software.wings.service.impl.splunk;

import com.splunk.HttpService;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import software.wings.beans.SplunkConfig;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import java.net.URI;
import java.net.UnknownHostException;

/**
 * Created by rsingh on 6/30/17.
 */
public class SplunkDelegateServiceImpl implements SplunkDelegateService {
  @Override
  public void validateConfig(SplunkConfig splunkConfig) {
    try {
      final ServiceArgs loginArgs = new ServiceArgs();
      loginArgs.setUsername(splunkConfig.getUsername());
      loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));

      final URI uri = new URI(splunkConfig.getUrl());
      loginArgs.setHost(uri.getHost());
      loginArgs.setPort(uri.getPort());

      if (uri.getScheme().equals("https")) {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      }
      Service.connect(loginArgs);
    } catch (Throwable t) {
      if (t.getCause() instanceof UnknownHostException) {
        throw new RuntimeException(splunkConfig.getUrl() + " is unreachable");
      }
      throw new RuntimeException(t.getMessage());
    }
  }
}
