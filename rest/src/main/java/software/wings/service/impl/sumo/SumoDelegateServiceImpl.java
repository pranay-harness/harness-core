package software.wings.service.impl.sumo;

import com.google.inject.Inject;

import com.sumologic.client.Credentials;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import software.wings.beans.SumoConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public class SumoDelegateServiceImpl implements SumoDelegateService {
  @Inject private EncryptionService encryptionService;
  @Override
  public void validateConfig(SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    try {
      getSumoClient(sumoConfig, encryptedDataDetails).search("*exception*");
    } catch (Throwable t) {
      if (t instanceof MalformedURLException) {
        throw new WingsException(sumoConfig.getSumoUrl() + " is not a valid url. ", t);
      }
      if (t instanceof SumoServerException) {
        SumoServerException sumoServerException = (SumoServerException) t;
        throw new WingsException(
            "Error from Sumo xxxxxxxx " + sumoServerException.getHTTPStatus() + " - " + sumoServerException.getMessage(),
            sumoServerException);
      }
      throw new WingsException("An error occurred connecting to Sumo xxxxxxxx " + t.getMessage(), t);
    }
  }

  SumoLogicClient getSumoClient(SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws MalformedURLException {
    encryptionService.decrypt(sumoConfig, encryptedDataDetails);
    final Credentials credentials =
        new Credentials(new String(sumoConfig.getAccessId()), new String(sumoConfig.getAccessKey()));
    SumoLogicClient sumoLogicClient = new SumoLogicClient(credentials);
    sumoLogicClient.setURL(sumoConfig.getSumoUrl());
    return sumoLogicClient;
  }
}
