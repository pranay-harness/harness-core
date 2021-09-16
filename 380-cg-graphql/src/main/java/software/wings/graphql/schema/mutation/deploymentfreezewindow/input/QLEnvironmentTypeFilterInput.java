package software.wings.graphql.schema.mutation.deploymentfreezewindow.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC) public enum QLEnvironmentTypeFilterInput { ALL, ALL_PROD, ALL_NON_PROD, CUSTOM }
