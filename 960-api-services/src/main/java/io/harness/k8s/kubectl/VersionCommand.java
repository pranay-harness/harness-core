/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

public class VersionCommand extends AbstractExecutable {
  public VersionCommand(Kubectl client) {
    this.client = client;
  }

  Kubectl client;

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("version");

    return command.toString();
  }
}
