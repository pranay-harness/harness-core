/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
