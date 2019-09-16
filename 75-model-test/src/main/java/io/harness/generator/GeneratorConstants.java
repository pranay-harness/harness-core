package io.harness.generator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GeneratorConstants {
  public final String AWS_TEST_LAMBDA_ROLE = "arn:aws:iam::448640225317:role/service-role/LambdaTestRole";
  public final String AWS_LAMBDA_ARTIFACT_S3BUCKET = "harness-example";
  public final String AWS_LAMBDA_ARTIFACT_PATH = "lambda/function.zip";
}
