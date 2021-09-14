/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import software.wings.beans.ConfigFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
public interface DelegateConfigService {
  List<ConfigFile> getConfigFiles(String appId, String envId, String uuid, String hostId, String accountId)
      throws IOException;
}
