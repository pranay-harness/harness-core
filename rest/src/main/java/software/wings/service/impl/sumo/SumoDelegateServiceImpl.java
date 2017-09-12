package software.wings.service.impl.sumo;

import com.sumologic.client.Credentials;
import com.sumologic.client.SumoClientException;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import software.wings.beans.SumoConfig;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public class SumoDelegateServiceImpl implements SumoDelegateService {
  @Override
  public void validateConfig(SumoConfig sumoConfig) throws IOException {
    try {
      getSumoClient(sumoConfig).search("*exception*");
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

  SumoLogicClient getSumoClient(SumoConfig sumoConfig) throws MalformedURLException {
    final Credentials credentials =
        new Credentials(new String(sumoConfig.getAccessId()), new String(sumoConfig.getAccessKey()));
    SumoLogicClient sumoLogicClient = new SumoLogicClient(credentials);
    sumoLogicClient.setURL(sumoConfig.getSumoUrl());
    return sumoLogicClient;
  }
}
