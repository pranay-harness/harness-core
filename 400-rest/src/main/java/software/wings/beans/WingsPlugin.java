/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import software.wings.settings.SettingValue;

import com.github.zafarkhaja.semver.Version;
import java.util.List;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public interface WingsPlugin {
  String getType();
  Class<? extends SettingValue> getSettingClass();
  List<PluginCategory> getPluginCategories();
  boolean isEnabled();
  Version getVersion();
}
