/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.embed.AwsLambdaDetails;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;

import java.util.List;

/**
 * Created by Pranjal on 01/29/2019
 */
@OwnedBy(CDP)
public interface AwsLambdaHelperServiceManager {
  List<String> listLambdaFunctions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  AwsLambdaDetails getFunctionDetails(AwsLambdaDetailsRequest request);
}
