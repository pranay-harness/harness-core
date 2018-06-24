package software.wings.service.impl.splunk;

import com.google.inject.Inject;

import com.splunk.HttpService;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SplunkConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.utils.Misc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 6/30/17.
 */
public class SplunkDelegateServiceImpl implements SplunkDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(SplunkDelegateServiceImpl.class);

  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);
  @Inject private EncryptionService encryptionService;
  @Override
  public boolean validateConfig(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      encryptionService.decrypt(splunkConfig, encryptedDataDetails);
      logger.info("Validating splunk, url {}, for user {} ", splunkConfig.getSplunkUrl(), splunkConfig.getUsername());
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
      return true;
    } catch (MalformedURLException exception) {
      throw new WingsException(splunkConfig.getSplunkUrl() + " is not a valid url", exception);
    } catch (Exception exception) {
      throw new WingsException("Error connecting to Splunk " + Misc.getMessage(exception), exception);
    }
  }
}
