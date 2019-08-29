package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.joda.time.DateTime;
import software.wings.service.impl.analysis.TimeSeriesRawData;
import software.wings.service.impl.analysis.TimeSeriesRawData.TimeSeriesRawDataKeys;
import software.wings.service.intfc.DataStoreService;

import java.util.Date;
import java.util.List;

@Slf4j
public class AddValidUntilToTimeSeriesRawData implements Migration {
  @Inject private DataStoreService dataStoreService;

  @Override
  public void migrate() {
    int offset = 0;
    int pageSize = 1000;
    while (true) {
      try {
        PageRequest<TimeSeriesRawData> pageRequest = aPageRequest()
                                                         .withOffset(String.valueOf(offset))
                                                         .withLimit(String.valueOf(pageSize))
                                                         .addOrder(TimeSeriesRawDataKeys.createdAt, ASC)
                                                         .build();
        PageResponse<TimeSeriesRawData> response = dataStoreService.list(TimeSeriesRawData.class, pageRequest);
        if (isEmpty(response.getResponse())) {
          logger.info("No more documents left to update!");
          break;
        }
        offset += pageSize;
        List<TimeSeriesRawData> rawDataList = response.getResponse();
        rawDataList.forEach(rawData
            -> rawData.setValidUntil(new Date(new DateTime(rawData.getLastUpdatedAt()).plusMonths(6).getMillis())));

        dataStoreService.save(TimeSeriesRawData.class, rawDataList, false);
        sleep(ofMillis(1500));
        logger.info("Updated {} time series raw data records", offset);
      } catch (Exception e) {
        logger.info("Exception while adding valid until to time sereis raw data", e);
        break;
      }
    }
  }
}
