/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.SshCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
@OwnedBy(CDP)
public abstract class SshCommandUnitYamlHandler<Y extends SshCommandUnit.Yaml, C extends SshCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
