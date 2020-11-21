package io.harness.delegate.task.manifests.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ManifestCollectionPTaskClientParamsKeys")
@OwnedBy(CDC)
public class ManifestCollectionPTaskClientParams {
  String appManifestId;
  String appId;
}
