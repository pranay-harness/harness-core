package software.wings.service.impl.analysis;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by sriram_parthasarathy on 10/14/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class TimeSeriesMLAnalysisRecordsTests extends WingsBaseTest {
  @Test
  public void testJsonParsing() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/TimeSeriesNRAnalysisRecords.json");
    String jsonTxt = IOUtils.toString(is);
    TimeSeriesMLAnalysisRecord records = JsonUtils.asObject(jsonTxt, TimeSeriesMLAnalysisRecord.class);
    assert (records.getTransactions().size() == 1);
    assert (records.getTransactions().get("0").getMetrics().size() == 1);
    assert (records.getTransactions().get("0").getMetrics().get("2").getControl().getData().size() > 0);
    assert (records.getTransactions().get("0").getMetrics().get("2").getTest().getData().size() > 0);
    assert (records.getTransactions().get("0").getMetrics().get("2").getControl().getWeights().size() > 0);
    assert (records.getTransactions().get("0").getMetrics().get("2").getTest().getWeights().size() > 0);
    assert (records.getTransactions().get("0").getMetrics().get("2").getResults().size() == 1);
    TimeSeriesMLHostSummary data =
        records.getTransactions().get("0").getMetrics().get("2").getResults().get("ip-172-31-0-38.harness.io");
    assert (data.getControl_cuts().size() > 0);
    assert (data.getTest_cuts().size() > 0);
    assert (data.getDistance().size() > 0);
    assert (data.getRisk() == 2);
    assert (Double.compare(data.getScore(), 3.75) == 0);
  }
}
