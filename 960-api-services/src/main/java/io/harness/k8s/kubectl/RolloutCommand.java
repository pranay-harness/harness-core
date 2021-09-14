/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

public class RolloutCommand extends AbstractExecutable {
  private Kubectl client;

  public RolloutCommand(Kubectl client) {
    this.client = client;
  }

  public RolloutStatusCommand status() {
    return new RolloutStatusCommand(this);
  }

  public RolloutHistoryCommand history() {
    return new RolloutHistoryCommand(this);
  }

  public RolloutUndoCommand undo() {
    return new RolloutUndoCommand(this);
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("rollout ");

    return command.toString();
  }
}
