/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerResizeCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public abstract class ContainerResizeCommandUnitYamlHandler<Y extends ContainerResizeCommandUnit.Yaml, C
                                                                extends ContainerResizeCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
