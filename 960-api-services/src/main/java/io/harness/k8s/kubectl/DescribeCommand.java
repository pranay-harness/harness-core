/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import org.apache.commons.lang3.StringUtils;

public class DescribeCommand extends AbstractExecutable {
  private Kubectl client;
  private String resource;
  private String filename;
  private String namespace;

  public DescribeCommand(Kubectl client) {
    this.client = client;
  }

  public DescribeCommand resource(String resource) {
    this.resource = resource;
    return this;
  }

  public DescribeCommand filename(String filename) {
    this.filename = filename;
    return this;
  }

  public DescribeCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(this.client.command()).append("describe ");

    if (StringUtils.isNotBlank(this.resource)) {
      command.append(this.resource).append(' ');
    }

    if (StringUtils.isNotBlank(this.filename)) {
      command.append(Kubectl.option(Option.filename, encloseWithQuotesIfNeeded(this.filename)));
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    return command.toString().trim();
  }
}
