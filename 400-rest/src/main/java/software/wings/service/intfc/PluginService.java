/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import software.wings.beans.AccountPlugin;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public interface PluginService {
  List<AccountPlugin> getInstalledPlugins(String accountId);

  Map<String, Map<String, Object>> getPluginSettingSchema(String accountId);
}
