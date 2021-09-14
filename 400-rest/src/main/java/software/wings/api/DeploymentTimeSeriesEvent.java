/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;

import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * This event is used for capturing deployment information.
 * @author rktummala on 08/12/19
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "deploymentTimeSeriesEventQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DeploymentTimeSeriesEvent extends Queuable {
  private TimeSeriesEventInfo timeSeriesEventInfo;
}
