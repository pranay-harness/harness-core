/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cache; // Cacheable in the key value sense object.
//
// Using key value storage is very commonly used to store cache objects. It is a nice separation of the
// concern between the system that store the values and the systems that generate it.
//
// Unfortunately it is very easy to forget to involve every aspect that affect the calculated value in the key.
// This makes using such system error prone, especially the affecting context element is rarely changes.
//
// To avoid neglecting this aspect of the cache we introducing the context hash. It has to incorporate all the
// context values involved in the generation of the object.

import java.util.List;

public interface Cacheable {
  // Returns a key that represents the object
  String key();

  List<String> parameters();
}
