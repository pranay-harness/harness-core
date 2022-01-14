package io.harness.batch.processing.tasklet.reader;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BillingDataReader {
  private String accountId;
  private Instant startTime;
  private Instant endTime;
  private int batchSize;
  private int offset;
  private BillingDataServiceImpl billingDataService;

  @Autowired private BatchJobType batchJobType;

  @Autowired
  public BillingDataReader(BillingDataServiceImpl billingDataService, String accountId, Instant startTime,
      Instant endTime, int batchSize, int offset, BatchJobType batchJobType) {
    this.accountId = accountId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.batchSize = batchSize;
    this.offset = offset;
    this.billingDataService = billingDataService;
    this.batchJobType = batchJobType;
  }

  public List<InstanceBillingData> getNext() {
    List<InstanceBillingData> instanceBillingDataList =
        billingDataService.read(accountId, startTime, endTime, batchSize, offset, batchJobType);
    if (!instanceBillingDataList.isEmpty()) {
      offset += batchSize;
    }
    return instanceBillingDataList;
  }
}
