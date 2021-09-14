/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerSetupCommandUnit;

/**
 * @author brett on 11/28/17
 */
public abstract class ContainerSetupCommandUnitYamlHandler<Y extends ContainerSetupCommandUnit.Yaml, C
                                                               extends ContainerSetupCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
