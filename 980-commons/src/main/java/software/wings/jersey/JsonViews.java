/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
