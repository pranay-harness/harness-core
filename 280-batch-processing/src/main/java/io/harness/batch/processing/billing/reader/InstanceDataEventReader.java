package io.harness.batch.processing.billing.reader;

import io.harness.ccm.commons.entities.batch.InstanceData;

import org.springframework.batch.item.ItemReader;

public interface InstanceDataEventReader {
  ItemReader<InstanceData> getEventReader(String accountId, Long startDate, Long endDate);
}
