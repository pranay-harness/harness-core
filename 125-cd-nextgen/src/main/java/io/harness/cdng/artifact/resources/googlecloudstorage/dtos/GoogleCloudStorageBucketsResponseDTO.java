/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.googlecloudstorage.dtos;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
@Schema(name = "GoogleCloudStorageBucketsResponseDTO", description = "Gcs response with list of buckets")
public class GoogleCloudStorageBucketsResponseDTO {
  List<GoogleCloudStorageBucketDetails> buckets;
}