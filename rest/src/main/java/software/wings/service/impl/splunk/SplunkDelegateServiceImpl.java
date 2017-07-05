package software.wings.service.impl.splunk;

import com.splunk.HttpService;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import software.wings.beans.SplunkConfig;
import software.wings.service.intfc.splunk.SplunkDelegateService;

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
      loginArgs.setHost(splunkConfig.getHost());
      loginArgs.setPort(splunkConfig.getPort());

      HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      Service.connect(loginArgs);
    } catch (Throwable t) {
      if (t.getCause() instanceof UnknownHostException) {
        throw new RuntimeException("host " + splunkConfig.getHost() + " is unreachable");
      }
      throw new RuntimeException(t.getMessage());
    }
  }
}
