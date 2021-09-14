/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.chartmuseum;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class ChartMuseumConstants {
  public final int CHART_MUSEUM_SERVER_START_RETRIES = 5;
  public final int PORTS_START_POINT = 35000;
  public final int PORTS_BOUND = 5000;
  public final int SERVER_HEALTH_CHECK_RETRIES = 12;
  public final int HEALTH_CHECK_TIME_GAP_SECONDS = 5; // 12*5 = 60 seconds is the timeout for health check

  public final String CHART_MUSEUM_SERVER_URL = "http://127.0.0.1:${PORT}";

  public final String NO_SUCH_BBUCKET_ERROR_CODE = "NoSuchBucket";
  public final String NO_SUCH_BBUCKET_ERROR = "NoSuchBucket: The specified bucket does not exist";

  public final String INVALID_ACCESS_KEY_ID_ERROR_CODE = "InvalidAccessKeyId";
  public final String INVALID_ACCESS_KEY_ID_ERROR =
      "InvalidAccessKeyId: The AWS Access Key Id you provided does not exist in our records.";

  public final String SIGNATURE_DOES_NOT_MATCH_ERROR_CODE = "SignatureDoesNotMatch";
  public final String SIGNATURE_DOES_NOT_MATCH_ERROR =
      "SignatureDoesNotMatch: The request signature we calculated does not match the signature you provided. Check your key and signing method";

  public final String BUCKET_REGION_ERROR_CODE = "BucketRegionError";

  public final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  public final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";

  public final String AMAZON_S3_COMMAND_TEMPLATE =
      " --port=${PORT} --storage=amazon --storage-amazon-bucket=${BUCKET_NAME} --storage-amazon-prefix=${FOLDER_PATH} --storage-amazon-region=${REGION}";

  public final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";

  public final String GCS_COMMAND_TEMPLATE =
      " --port=${PORT} --storage=google --storage-google-bucket=${BUCKET_NAME} --storage-google-prefix=${FOLDER_PATH}";
}
