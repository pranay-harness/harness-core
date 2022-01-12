/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cache;

import java.io.Serializable;

// Cacheable in a distributed store object.
//
// Distributed cache is mostly used for heavy to calculate objects based on too much data
// involved in the calculation, like different forms of aggregations or expensive algorithm
// that involve too much calculations or similar.
//
// For such objects though we would like to preserve and reuse the cache as long as possible.
// This creates a need of versioning. Long living cache could have wrong data structure or it
// could be calculated with outdated algorithm.
//
// Invalidating the cache with every deployment is not great, first because it will force
// constant cache recalculation, assuming frequent deployments and second it will create
// hard to manage transition period while two versions of the service are still alive.
//
// Maintaining backward and forward compatibility for a cache requires a lot of engineering
// effort and seems too expensive for something that can easily be calculated again.
//
// A great middle ground is tracking for changes for a particular object type. This will
// allow for frequent updates, without expensive cache recalculation and at the same time
// will not require backward and forward compatibility maintenance.
//
// To achieve this all we need to do is to extend the object key with the object structure
// identification and the id of the algorithm used for the calculation.
//
// Naturally if any one of these two is different from a previous one the object will not be
// found in the cache and a new version will be generated. The previous one will be garbage
// collected being unused anymore.

public interface Distributable extends Cacheable, Serializable {
  // Identification of the data structure. Note that for the sack of the distribution to works
  // there is not any other requirement for the id but to be different from the previous one.
  // This makes using the java native SerialVersionUID as structure identification. What is more
  // we do not even need to maintain it. When something changes directly or indirectly in the
  // structure, the hash will automatically differ from the previous one, making the following
  // implementation the recommended one:
  //
  // public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(Dummy.class).getSerialVersionUID();
  //
  // @Override
  // public long structureHash() {
  //   return structureHash;
  // }
  long structureHash();

  // Identification of the algorithm.
  long algorithmId();
}
