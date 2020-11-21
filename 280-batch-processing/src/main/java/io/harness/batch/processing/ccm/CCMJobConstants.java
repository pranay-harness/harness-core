package io.harness.batch.processing.ccm;

import java.time.Instant;
import org.springframework.batch.core.JobParameters;

public class CCMJobConstants {
  public static final String JOB_ID = "JobID";
  public static final String ACCOUNT_ID = "accountId";
  public static final String JOB_END_DATE = "endDate";
  public static final String JOB_START_DATE = "startDate";
  public static final String BATCH_JOB_TYPE = "batchJobType";

  private CCMJobConstants() {}

  public static Instant getFieldValueFromJobParams(JobParameters parameters, String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }

  public static Long getFieldLongValueFromJobParams(JobParameters parameters, String fieldName) {
    return Long.valueOf(parameters.getString(fieldName));
  }

  public static BatchJobType getBatchJobTypeFromJobParams(JobParameters parameters, String fieldName) {
    return BatchJobType.valueOf(parameters.getString(fieldName));
  }
}
