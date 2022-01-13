/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.jersey;

import lombok.experimental.UtilityClass;

/**
 * Created by peeyushaggarwal on 3/1/17.
 */
@UtilityClass
public class JsonViews {
  public static class Public {}
  public static class Internal extends Public {}
}
