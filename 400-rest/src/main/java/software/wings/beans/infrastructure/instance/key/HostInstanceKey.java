/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.key;

import io.harness.mongo.index.FdIndex;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Host based instance key like physical host and cloud instances like ec2 , gcp instance.
 * @author rktummala on 09/05/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HostInstanceKey extends InstanceKey {
  @FdIndex private String hostName;
  @FdIndex private String infraMappingId;
}
