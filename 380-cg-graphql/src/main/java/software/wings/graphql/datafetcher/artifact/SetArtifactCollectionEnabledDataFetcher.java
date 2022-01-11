/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.artifact.QLSetArtifactCollectionEnabledInput;
import software.wings.graphql.schema.mutation.artifact.QLSetArtifactCollectionEnabledPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AuthService;

import com.google.inject.Inject;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class SetArtifactCollectionEnabledDataFetcher
    extends BaseMutatorDataFetcher<QLSetArtifactCollectionEnabledInput, QLSetArtifactCollectionEnabledPayload> {
  private AuthService authService;
  private ArtifactStreamService artifactStreamService;

  @Inject
  public SetArtifactCollectionEnabledDataFetcher(ArtifactStreamService artifactStreamService, AuthService authService) {
    super(QLSetArtifactCollectionEnabledInput.class, QLSetArtifactCollectionEnabledPayload.class);
    this.authService = authService;
    this.artifactStreamService = artifactStreamService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.SERVICE)
  protected QLSetArtifactCollectionEnabledPayload mutateAndFetch(
      QLSetArtifactCollectionEnabledInput parameter, MutationContext mutationContext) {
    ArtifactStream artifactStream = artifactStreamService.get(parameter.getArtifactStreamId());

    if (artifactStream == null) {
      return QLSetArtifactCollectionEnabledPayload.builder()
          .message("Artifact stream not found for the id: " + parameter.getArtifactStreamId())
          .clientMutationId(parameter.getClientMutationId())
          .build();
    }

    String accountId = artifactStream.getAccountId();

    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorize(accountId, artifactStream.getAppId(), artifactStream.getServiceId(), user,
          Collections.singletonList(new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
              PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.UPDATE)),
          true);
    }

    artifactStreamService.updateCollectionEnabled(artifactStream, parameter.getArtifactCollectionEnabled());

    return QLSetArtifactCollectionEnabledPayload.builder()
        .message("Successfully set artifact collection enabled to " + parameter.getArtifactCollectionEnabled())
        .clientMutationId(parameter.getClientMutationId())
        .build();
  }
}
