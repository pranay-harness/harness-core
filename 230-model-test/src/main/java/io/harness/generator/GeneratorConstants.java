/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GeneratorConstants {
  public final String AWS_TEST_LAMBDA_ROLE = "arn:aws:iam::479370281431:role/lambda-role";

  public final String AWS_LAMBDA_ARTIFACT_S3BUCKET = "lambda-harness-tutorial";
  public final String AWS_LAMBDA_ARTIFACT_PATH = "function.zip";
}
