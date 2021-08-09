package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

// TODO : Rename this to OrchestrationConfigConstants
@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationPublisherName {
  public static final String PUBLISHER_NAME = "orchestrationPublisherName";

  public static final String PERSISTENCE_LAYER = "wePersistenceLayer";
}
