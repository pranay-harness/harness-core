/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import io.harness.beans.EnvironmentType;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.AmiServiceSetupElement;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.sm.ExecutionContext;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AmiResizeTaskRequestData {
  ExecutionContext context;
  private String appId;
  private String accountId;
  private String envId;
  private EnvironmentType environmentType;
  private String activityId;
  private String region;
  private String commandName;
  private AwsConfig awsConfig;
  private List<EncryptedDataDetail> encryptionDetails;
  private boolean resizeNewFirst;
  private String newAutoScalingGroupName;
  private Integer newAsgFinalDesiredCount;
  private List<AwsAmiResizeData> resizeData;
  private List<String> classicLBs;
  private List<String> targetGroupArns;
  private boolean rollback;
  private AmiServiceSetupElement serviceSetupElement;
}
