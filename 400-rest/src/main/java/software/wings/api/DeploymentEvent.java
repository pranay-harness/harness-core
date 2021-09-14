/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;

import software.wings.api.ondemandrollback.OnDemandRollbackInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * This is used as request for capturing deployment and instance information.
 * @author rktummala on 02/04/18
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "deploymentEventQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DeploymentEvent extends Queuable {
  private boolean isRollback;
  private List<DeploymentSummary> deploymentSummaries;
  private OnDemandRollbackInfo onDemandRollbackInfo;
}
