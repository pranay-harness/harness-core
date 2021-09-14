/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.bigQuery;

import com.google.cloud.bigquery.BigQuery;

public interface BigQueryService {
  BigQuery get();
}
