/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.intfc;

import java.util.List;

public interface InstanceDataBulkWriteService {
  @SuppressWarnings("unchecked") boolean updateList(List<?> objectList);
}
