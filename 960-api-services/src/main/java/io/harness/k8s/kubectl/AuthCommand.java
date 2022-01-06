/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import org.apache.commons.lang3.StringUtils;

public class AuthCommand extends AbstractExecutable {
  private Kubectl client;
  private String verb;
  private String resources;
  private String namespace;
  private boolean allNamespaces;

  public AuthCommand(Kubectl client) {
    this.client = client;
  }

  public AuthCommand verb(String verb) {
    this.verb = verb;
    return this;
  }

  public AuthCommand resources(String resources) {
    this.resources = resources;
    return this;
  }

  public AuthCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public AuthCommand allNamespaces(boolean allNamespaces) {
    this.allNamespaces = allNamespaces;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("auth can-i ");

    if (StringUtils.isNotBlank(this.verb)) {
      command.append(this.verb).append(' ');
    }

    if (StringUtils.isNotBlank(this.resources)) {
      command.append(this.resources).append(' ');
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    if (this.allNamespaces) {
      command.append(Kubectl.option(Option.allNamespaces, "true"));
    }

    return command.toString().trim();
  }
}
