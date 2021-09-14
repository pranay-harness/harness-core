/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.changestreams;

/**
 * Possible types of operations that can take place.
 *
 * @author utkarsh
 */

public enum ChangeType { INSERT, DELETE, REPLACE, UPDATE, INVALIDATE }
