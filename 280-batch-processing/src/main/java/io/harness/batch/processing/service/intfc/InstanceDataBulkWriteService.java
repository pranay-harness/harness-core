package io.harness.batch.processing.service.intfc;

import java.util.List;

public interface InstanceDataBulkWriteService {
  @SuppressWarnings("unchecked") boolean updateList(List<?> objectList);
}
