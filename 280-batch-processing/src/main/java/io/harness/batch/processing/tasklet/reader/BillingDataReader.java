package io.harness.batch.processing.tasklet.reader;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

@Slf4j
public class BillingDataReader {
  private String accountId;
  private Instant startTime;
  private Instant endTime;
  private int batchSize;
  private int offset;
  private BillingDataServiceImpl billingDataService;

  @Autowired
  public BillingDataReader(BillingDataServiceImpl billingDataService, String accountId, Instant startTime,
      Instant endTime, int batchSize, int offset) {
    this.accountId = accountId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.batchSize = batchSize;
    this.offset = offset;
    this.billingDataService = billingDataService;
  }

  public List<InstanceBillingData> getNext() {
    List<InstanceBillingData> instanceBillingDataList =
        billingDataService.read(accountId, startTime, endTime, batchSize, offset);
    if (!instanceBillingDataList.isEmpty()) {
      offset += batchSize;
    }
    return instanceBillingDataList;
  }
}
