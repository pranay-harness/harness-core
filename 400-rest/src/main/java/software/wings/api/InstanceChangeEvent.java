/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;

import software.wings.beans.infrastructure.instance.Instance;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * This is a wrapper class of Instance to make it extend queuable.
 * This is used as request for capturing instance information.
 * @author rktummala on 08/24/17
 *
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "instanceChangeQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class InstanceChangeEvent extends Queuable {
  private List<Instance> instanceList;
  private List<String> autoScalingGroupList;
  private String appId;
}
