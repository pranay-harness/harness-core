/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import java.util.HashSet;
import java.util.Set;

/**
 * CauseCollection is a helper class that overcomes the java limitation of having only one cause throwable.
 *
 * It simply abuses the system attaching every next cause as they are in a chain.
 */
public class CauseCollection {
  private static final int CAUSE_LIMIT = 50;

  private Throwable first;
  private Throwable last;

  private Set<Throwable> deduplicate = new HashSet<>();

  public CauseCollection addCause(Throwable cause) {
    // For the incoming chain make sure that we start from the first that is not already hooked.
    while (cause != null && deduplicate.contains(cause)) {
      cause = cause.getCause();
    }

    // If there is no new throwable, there is nothing to do.
    if (cause == null) {
      return this;
    }

    // Lets find how many new throwables we are about to add.
    Throwable current = cause;
    int count = 0;
    while (current != null) {
      current = current.getCause();
      ++count;
    }

    // If this will make the collection to exceed the limit lets prevent that.
    if (deduplicate.size() + count > CAUSE_LIMIT) {
      return this;
    }

    // If this is the first time we adding cause, init the returning cause with it.
    if (first == null) {
      first = last = cause;
    } else {
      last.initCause(cause);
    }

    // Lets go over the attached collection and hash it for de-duplication purposes.
    while (last.getCause() != null) {
      deduplicate.add(last);
      last = last.getCause();
    }

    deduplicate.add(last);
    return this;
  }

  public Throwable getCause() {
    return first;
  }
}
