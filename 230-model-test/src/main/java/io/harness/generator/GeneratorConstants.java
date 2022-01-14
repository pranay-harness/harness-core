package io.harness.generator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GeneratorConstants {
  public final String AWS_TEST_LAMBDA_ROLE = "arn:aws:iam::479370281431:role/lambda-role";

  public final String AWS_LAMBDA_ARTIFACT_S3BUCKET = "lambda-harness-tutorial";
  public final String AWS_LAMBDA_ARTIFACT_PATH = "function.zip";
}
