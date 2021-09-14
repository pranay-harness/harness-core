/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation to ignore unused index check for a database collection.
 * Verification service stores some collections outside of mongo so those collections indexes never get used.
 * Please be careful on using this index and add it to your collection only if you are not using mongo to store and
 * retrieve data for the collection
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IgnoreUnusedIndex {}
