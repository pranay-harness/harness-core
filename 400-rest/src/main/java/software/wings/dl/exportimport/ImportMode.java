/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.dl.exportimport;

/**
 * This enum defines 3 different import modes.
 *
 * @author marklu on 11/15/18
 */
public enum ImportMode {
  // This mode will dry run the import process and report possible id/natural key clashes, number of records each entity
  // type to be imported.
  DRY_RUN,
  // This mode will update existing records with same id and natural key, and insert new records
  UPSERT,
  // This mode will only insert new records and leave existing records with the same id/natural key untouched.
  INSERT
}
