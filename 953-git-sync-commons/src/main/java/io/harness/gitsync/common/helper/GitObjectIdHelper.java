/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
