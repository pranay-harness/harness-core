/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.entityinterface;

import software.wings.beans.HarnessTagLink;

import java.util.List;

public interface TagAware {
  List<HarnessTagLink> getTagLinks();
  void setTagLinks(List<HarnessTagLink> tagLinks);
}
