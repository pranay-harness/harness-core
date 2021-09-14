/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.NoArgsConstructor;

/**
 * Base class for storing instance information based on type
 * @author rktummala on 08/25/17
 */
@NoArgsConstructor
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public abstract class InstanceInfo {}
