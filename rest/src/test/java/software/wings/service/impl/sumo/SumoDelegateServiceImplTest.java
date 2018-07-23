package software.wings.service.impl.sumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.sumologic.client.SumoClientException;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SumoConfig;
import software.wings.exception.WingsException;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Collections;

/**
 * Created by sriram_parthasarathy on 9/12/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class SumoDelegateServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(SumoDelegateServiceImplTest.class);

  @Mock SumoConfig sumoConfig;

  @Mock SumoLogicClient sumoLogicClient;

  @Mock EncryptionService encryptionService;

  @Before
  public void setUp() {
    when(sumoConfig.getAccessId()).thenReturn("1234".toCharArray());
    when(sumoConfig.getAccessKey()).thenReturn("3456".toCharArray());
    when(sumoConfig.getSumoUrl()).thenReturn("https://localhost:9000/");
  }

  @Test
  public void testValidateConfigBadUrl() throws IOException {
    when(sumoConfig.getSumoUrl()).thenReturn("htt//localhost:9000/");
    SumoDelegateServiceImpl sumoDelegateService = new SumoDelegateServiceImpl();
    setInternalState(sumoDelegateService, "encryptionService", new EncryptionServiceImpl());
    String exceptionMsg = "";
    try {
      sumoDelegateService.validateConfig(sumoConfig, Collections.emptyList());
    } catch (WingsException e) {
      exceptionMsg = e.getMessage();
    }
    assertThat(exceptionMsg).contains("is not a valid url");
  }

  @Test(expected = SumoClientException.class)
  public void testSumoException() throws IOException {
    doThrow(new SumoServerException("https://localhost:9000/", "{\"message\": \"This is broken\"}"))
        .when(sumoConfig)
        .getSumoUrl();
    SumoDelegateServiceImpl sumoDelegateService = new SumoDelegateServiceImpl();
    String msg = "This is broken";
    String exceptionMsg = "";
    try {
      sumoDelegateService.validateConfig(sumoConfig, Collections.emptyList());
    } catch (WingsException ex) {
      exceptionMsg = ex.getMessage();
    }
    assertThat(exceptionMsg).isEqualTo(msg);
  }
}
