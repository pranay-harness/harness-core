/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helm;

public enum HelmCliCommandType {
  INSTALL,
  UPGRADE,
  ROLLBACK,
  RELEASE_HISTORY,
  DELETE_RELEASE,
  LIST_RELEASE,
  REPO_ADD,
  REPO_UPDATE,
  REPO_LIST,
  SEARCH_REPO,
  VERSION,
  REPO_ADD_CHART_MEUSEUM,
  REPO_ADD_HTTP,
  FETCH,
  REPO_REMOVE,
  INIT,
  RENDER_CHART,
  RENDER_SPECIFIC_CHART_FILE,
  FETCH_ALL_VERSIONS;
}
