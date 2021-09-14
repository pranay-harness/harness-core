/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
