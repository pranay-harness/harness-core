package io.harness.cd;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class NGServiceConstants {
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String SERVICE_IDENTIFIER = "serviceId";
  public static final String STATUS = "status";
  public static final String TIME_ENTITY = "time_entity";
  public static final String NUMBER_OF_RECORDS = "numberOfRecords";
  public static final String BUCKET_SIZE_IN_DAYS = "bucketSizeInDays";
  public static final String PIPELINE_EXECUTION_ID = "pipeline_execution_summary_cd_id";
  public static final String ENVIRONMENT_TYPE = "environmentType";
}
