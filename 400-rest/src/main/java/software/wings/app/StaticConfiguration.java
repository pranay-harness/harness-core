/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 *
 */

package software.wings.app;

import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;

import software.wings.beans.Graph;

import com.google.common.io.Resources;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * The type Static configuration.
 *
 * @author Rishi
 */
@Singleton
public class StaticConfiguration {
  /**
   * Default simple workflow graph.
   *
   * @return graph graph
   */
  public Graph defaultSimpleWorkflow() {
    URL url = this.getClass().getResource("/configs/simple_workflow_default_graph.json");
    String json;
    try {
      json = Resources.toString(url, Charset.defaultCharset());
    } catch (IOException e) {
      throw new WingsException("Error in loading simple workflow default graph", e);
    }
    return JsonUtils.asObject(json, Graph.class);
  }
}
