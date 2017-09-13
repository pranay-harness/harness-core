package software.wings.service.impl.sumo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sumologic.client.SumoClientException;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.SumoConfig;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by sriram_parthasarathy on 9/12/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class SumoDelegateServiceImplTest {
  @Mock SumoConfig sumoConfig;

  @Mock SumoLogicClient sumoLogicClient;

  @Before
  public void setUp() {
    when(sumoConfig.getAccessId()).thenReturn("1234".toCharArray());
    when(sumoConfig.getAccessKey()).thenReturn("3456".toCharArray());
    when(sumoConfig.getSumoUrl()).thenReturn("https://localhost:9000/");
  }

  @Test(expected = MalformedURLException.class)
  public void testValidateConfigBadUrl() throws IOException {
    when(sumoConfig.getSumoUrl()).thenReturn("htt//localhost:9000/");
    SumoDelegateServiceImpl sumoDelegateService = new SumoDelegateServiceImpl();
    sumoDelegateService.validateConfig(sumoConfig);
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
      sumoDelegateService.validateConfig(sumoConfig);
    } catch (RuntimeException ex) {
      exceptionMsg = ex.getMessage();
    }

    assertEquals(msg, exceptionMsg);
  }

  @Test
  public void testValidateConfig() throws IOException {
    SumoDelegateServiceImpl sumoDelegateService = Mockito.spy(new SumoDelegateServiceImpl());
    doReturn(sumoLogicClient).when(sumoDelegateService).getSumoClient(sumoConfig);
    try {
      sumoDelegateService.validateConfig(sumoConfig);
    } catch (RuntimeException ex) {
    }
    verify(sumoDelegateService, times(1)).getSumoClient(sumoConfig);
    verify(sumoLogicClient, times(1)).search("*exception*");
  }
}
