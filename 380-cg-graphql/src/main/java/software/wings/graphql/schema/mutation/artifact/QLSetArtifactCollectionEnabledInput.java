/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.artifact;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "SetArtifactCollectionEnabledInputKeys")
public class QLSetArtifactCollectionEnabledInput {
  String appId;
  String artifactStreamId;
  Boolean artifactCollectionEnabled;
  String clientMutationId;
}
