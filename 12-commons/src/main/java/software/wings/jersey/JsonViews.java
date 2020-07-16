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
