package io.harness.batch.processing.ccm;

import org.springframework.batch.core.JobParameters;

import java.time.Instant;

public class CCMJobConstants {
  public static final String JOB_ID = "JobID";
  public static final String ACCOUNT_ID = "accountId";
  public static final String JOB_END_DATE = "endDate";
  public static final String JOB_START_DATE = "startDate";

  private CCMJobConstants() {}

  public static Instant getFieldValueFromJobParams(JobParameters parameters, String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }
}
