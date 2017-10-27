package software.wings.service.impl.sumo;

import com.sumologic.client.Credentials;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import software.wings.beans.SumoConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import javax.inject.Inject;

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
        throw new MalformedURLException(sumoConfig.getSumoUrl() + " is not a valid url");
      }
      if (t instanceof SumoServerException) {
        throw new RuntimeException(((SumoServerException) t).getErrorMessage());
      }
      throw new RuntimeException(t.getMessage());
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
