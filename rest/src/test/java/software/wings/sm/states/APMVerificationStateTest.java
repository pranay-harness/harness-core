package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.apm.APMParserTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class APMVerificationStateTest extends WingsBaseTest {
  @Test
  public void metricCollectionInfos() throws IOException {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    String textLoad = Resources.toString(APMParserTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    apmVerificationState.setMetricInfoList(textLoad);
    Map<String, List<APMMetricInfo>> apmMetricInfos = apmVerificationState.apmMetricInfos();
    assertEquals(1, apmMetricInfos.size());
    assertEquals(2, apmMetricInfos.get("query").size());
    assertNotNull(apmMetricInfos.get("query").get(0).getResponseMappers().get("txnName").getFieldValue());
    assertNotNull(apmMetricInfos.get("query").get(1).getResponseMappers().get("txnName").getJsonPath());
  }
}
