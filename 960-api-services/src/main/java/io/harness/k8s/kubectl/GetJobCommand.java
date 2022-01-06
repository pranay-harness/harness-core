/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GetJobCommand extends AbstractExecutable {
  private GetCommand getCommand;
  private String jobName;
  private String output;
  private String namespace;

  public GetJobCommand(GetCommand getCommand, String jobName, String namespace) {
    this.getCommand = getCommand;
    this.jobName = jobName;
    this.namespace = namespace;
  }

  public GetJobCommand output(String output) {
    this.output = output;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(getCommand.command())
        .append(' ')
        .append(ResourceType.jobs.toString())
        .append(' ')
        .append(jobName)
        .append(' ');

    if (isNotBlank(namespace)) {
      command.append(Kubectl.option(Option.namespace, namespace));
    }

    if (isNotBlank(output)) {
      command.append(Kubectl.option(Option.output, output));
    }

    return command.toString().trim();
  }
}
