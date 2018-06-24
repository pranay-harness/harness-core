package software.wings.service.impl.sumo;

import com.google.inject.Inject;

import com.sumologic.client.Credentials;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import io.harness.network.Http;
import io.harness.time.Timestamp;
import org.apache.http.HttpHost;
import software.wings.beans.SumoConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.sumo.SumoDelegateService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public class SumoDelegateServiceImpl implements SumoDelegateService {
  @Inject private EncryptionService encryptionService;
  @Override
  public boolean validateConfig(SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    try {
      getSumoClient(sumoConfig, encryptedDataDetails)
          .createSearchJob("*exception*", String.valueOf(Timestamp.currentMinuteBoundary() - 1),
              String.valueOf(Timestamp.currentMinuteBoundary()), TimeZone.getDefault().getID());
      return true;
    } catch (MalformedURLException exception) {
      throw new WingsException(sumoConfig.getSumoUrl() + " is not a valid url. ", exception);
    } catch (SumoServerException exception) {
      throw new WingsException(
          "Error from Sumo xxxxxxxx " + exception.getHTTPStatus() + " - " + Misc.getMessage(exception), exception);
    } catch (Exception exception) {
      throw new WingsException("An error occurred connecting to Sumo xxxxxxxx " + Misc.getMessage(exception), exception);
    }
  }

  SumoLogicClient getSumoClient(SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws MalformedURLException {
    encryptionService.decrypt(sumoConfig, encryptedDataDetails);
    final Credentials credentials =
        new Credentials(new String(sumoConfig.getAccessId()), new String(sumoConfig.getAccessKey()));
    SumoLogicClient sumoLogicClient = new SumoLogicClient(credentials);
    HttpHost httpProxyHost = Http.getHttpProxyHost(sumoConfig.getSumoUrl());
    if (httpProxyHost != null) {
      sumoLogicClient.setProxyHost(httpProxyHost.getHostName());
      sumoLogicClient.setProxyPort(httpProxyHost.getPort());
      sumoLogicClient.setProxyProtocol(httpProxyHost.getSchemeName());
    }
    sumoLogicClient.setURL(sumoConfig.getSumoUrl());
    return sumoLogicClient;
  }
}
