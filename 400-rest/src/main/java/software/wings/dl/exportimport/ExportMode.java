/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.dl.exportimport;

/**
 * @author marklu on 11/15/18
 */
public enum ExportMode {
  // This mode will export all account/app level entities.
  ALL,
  // This mode will export only a selected sub-set of exportable entities.
  SPECIFIC
}
