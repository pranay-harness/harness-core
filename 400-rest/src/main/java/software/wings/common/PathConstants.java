/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.common;

public interface PathConstants {
  /**
   * The constant WINGS_RUNTIME_PATH.
   */
  String WINGS_RUNTIME_PATH = "WINGS_RUNTIME_PATH";

  /**
   * The constant WINGS_STAGING_PATH.
   */
  String WINGS_STAGING_PATH = "WINGS_STAGING_PATH";

  /**
   * The constant WINGS_BACKUP_PATH.
   */
  String WINGS_BACKUP_PATH = "WINGS_BACKUP_PATH";

  /**
   * The constant DEFAULT_RUNTIME_PATH.
   */
  String DEFAULT_RUNTIME_PATH = "$HOME/${app.name}/${service.name}/${env.name}/runtime";
  /**
   * The constant DEFAULT_BACKUP_PATH.
   */
  String DEFAULT_BACKUP_PATH = "$HOME/${app.name}/${service.name}/${env.name}/backup/${timestampId}";
  /**
   * The constant DEFAULT_STAGING_PATH.
   */
  String DEFAULT_STAGING_PATH = "$HOME/${app.name}/${service.name}/${env.name}/staging/${timestampId}";
  /**
   * The constant DEFAULT_WINDOWS_RUNTIME_PATH.
   */
  String DEFAULT_WINDOWS_RUNTIME_PATH = "%USERPROFILE%\\${app.name}\\${service.name}\\${env.name}\\runtime";
  /**
   * The constant RUNTIME_PATH.
   */
  String RUNTIME_PATH = "RUNTIME_PATH";
  /**
   * The constant BACKUP_PATH.
   */
  String BACKUP_PATH = "BACKUP_PATH";
  /**
   * The constant STAGING_PATH.
   */
  String STAGING_PATH = "STAGING_PATH";
  /**
   * The constant WINDOWS_RUNTIME_PATH.
   */
  String WINDOWS_RUNTIME_PATH = "WINDOWS_RUNTIME_PATH";
}
