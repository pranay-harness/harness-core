/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.settings.SettingVariableTypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

/**
 * The Enum ArtifactStreamType.
 */
@OwnedBy(CDC)
public enum ArtifactStreamType {
  JENKINS,
  BAMBOO,
  DOCKER,
  ECR,
  GCR,
  ACR,
  NEXUS,
  ARTIFACTORY,
  AMAZON_S3,
  AMI,
  GCS,
  SMB,
  SFTP,
  AZURE_ARTIFACTS,
  AZURE_MACHINE_IMAGE,
  CUSTOM;

  public static final ImmutableMap<ArtifactStreamType, Set<SettingVariableTypes>> supportedSettingVariableTypes =
      ImmutableMap.<ArtifactStreamType, Set<SettingVariableTypes>>builder()
          .put(JENKINS, Sets.newHashSet(SettingVariableTypes.JENKINS))
          .put(BAMBOO, Sets.newHashSet(SettingVariableTypes.BAMBOO))
          .put(DOCKER, Sets.newHashSet(SettingVariableTypes.DOCKER))
          .put(ECR, Sets.newHashSet(SettingVariableTypes.ECR, SettingVariableTypes.AWS))
          .put(GCR, Sets.newHashSet(SettingVariableTypes.GCR, SettingVariableTypes.GCP))
          .put(ACR, Sets.newHashSet(SettingVariableTypes.ACR, SettingVariableTypes.AZURE))
          .put(NEXUS, Sets.newHashSet(SettingVariableTypes.NEXUS))
          .put(ARTIFACTORY, Sets.newHashSet(SettingVariableTypes.ARTIFACTORY))
          .put(AMAZON_S3, Sets.newHashSet(SettingVariableTypes.AMAZON_S3, SettingVariableTypes.AWS))
          .put(AMI, Sets.newHashSet(SettingVariableTypes.AWS))
          .put(GCS, Sets.newHashSet(SettingVariableTypes.GCS, SettingVariableTypes.GCP))
          .put(SMB, Sets.newHashSet(SettingVariableTypes.SMB))
          .put(SFTP, Sets.newHashSet(SettingVariableTypes.SFTP))
          .put(AZURE_ARTIFACTS, Sets.newHashSet(SettingVariableTypes.AZURE_ARTIFACTS_PAT, SettingVariableTypes.AZURE))
          .put(AZURE_MACHINE_IMAGE, Sets.newHashSet(SettingVariableTypes.AZURE))
          .put(CUSTOM, Sets.newHashSet(SettingVariableTypes.values()))
          .build();
}
