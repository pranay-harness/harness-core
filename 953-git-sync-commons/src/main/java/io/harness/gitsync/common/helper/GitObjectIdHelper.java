/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.helper;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class GitObjectIdHelper {
  public static String getObjectIdForString(String object) {
    final byte[] data = object.getBytes();
    ObjectInserter.Formatter f = new ObjectInserter.Formatter();
    ObjectId id = f.idFor(OBJ_BLOB, data);
    return id.getName();
  }
}
