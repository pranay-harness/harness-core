package software.wings.service.impl.splunk;

import com.splunk.HttpService;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import software.wings.beans.SplunkConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by rsingh on 6/30/17.
 */
public class SplunkDelegateServiceImpl implements SplunkDelegateService {
  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);
  @Inject private EncryptionService encryptionService;
  @Override
  public void validateConfig(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      encryptionService.decrypt(splunkConfig, encryptedDataDetails);
      final ServiceArgs loginArgs = new ServiceArgs();
      loginArgs.setUsername(splunkConfig.getUsername());
      loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));

      final URL url = new URL(splunkConfig.getSplunkUrl());
      loginArgs.setHost(url.getHost());
      loginArgs.setPort(url.getPort());

      if (url.toURI().getScheme().equals("https")) {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      }
      Service service = new Service(loginArgs);
      service.setConnectTimeout(HTTP_TIMEOUT);
      service.setReadTimeout(HTTP_TIMEOUT);

      Service.connect(loginArgs);
    } catch (Throwable t) {
      if (t instanceof MalformedURLException) {
        throw new WingsException(splunkConfig.getSplunkUrl() + " is not a valid url", t);
      }
      throw new WingsException("Error connecting to Splunk " + t.getMessage(), t);
    }
  }
}
