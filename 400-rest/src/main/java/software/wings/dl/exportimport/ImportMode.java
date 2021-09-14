/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
