/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import org.apache.commons.lang3.StringUtils;

public class RolloutStatusCommand extends AbstractExecutable {
  private RolloutCommand rolloutCommand;
  private String resource;
  private String namespace;
  private boolean watch;

  public RolloutStatusCommand(RolloutCommand rolloutCommand) {
    this.rolloutCommand = rolloutCommand;
  }

  public RolloutStatusCommand resource(String resource) {
    this.resource = resource;
    return this;
  }

  public RolloutStatusCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public RolloutStatusCommand watch(boolean watch) {
    this.watch = watch;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(rolloutCommand.command()).append("status ");

    if (StringUtils.isNotBlank(this.resource)) {
      command.append(this.resource).append(' ');
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    command.append(Kubectl.flag(Flag.watch, this.watch));

    return command.toString().trim();
  }
}
