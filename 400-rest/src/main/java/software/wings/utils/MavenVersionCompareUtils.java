/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * <p>Utility to compare version.</p>
 *
 * @author yclian
 * @since 20120507
 * @version 20120507
 */
public final class MavenVersionCompareUtils {
  private static final Pattern PATTERN_APPROXIMATION = compile("^([\\d\\.]+\\.)*(\\d+)$");

  private ComparableVersion mMavenVersionCompareUtil;

  private MavenVersionCompareUtils(String v) {
    mMavenVersionCompareUtil = new ComparableVersion(v);
  }

  public static MavenVersionCompareUtils compare(String v) {
    return new MavenVersionCompareUtils(v);
  }

  /**
   * <p>Return the result of {@link Comparable#compareTo(Object)}. Very limited as it doesn't support {@code &gt;=},
   * {@code &lt;=} and {@code ~&gt;}.</p>
   *
   * @param v
   * @return
   */
  public int with(String v) {
    return mMavenVersionCompareUtil.compareTo(new ComparableVersion(v));
  }

  public boolean eq(String v) {
    return with(v) == 0;
  }
  public boolean le(String v) {
    int c = with(v);
    return c == 0 || c == -1;
  }
  public boolean lt(String v) {
    return with(v) == -1;
  }
  public boolean ge(String v) {
    int c = with(v);
    return c == 0 || c == 1;
  }
  public boolean gt(String v) {
    return with(v) == 1;
  }

  /**
   * <p>Approximately greater than, inspired by (and works exactly like) RubyGems.</p>
   *
   * @see <a href="http://docs.rubygems.org/read/chapter/16">RubyGems Manuals - Specifying Versions</a>
   * @param v
   * @return
   */
  public boolean agt(String v) {
    return ge(v) && lt(approximateUpper(v));
  }

  private String approximateUpper(String v) {
    Matcher m = PATTERN_APPROXIMATION.matcher(v.split("\\.\\d+$")[0]);
    if (m.find()) {
      int i = Integer.parseInt(m.group(2));
      return (null != m.group(1) ? m.group(1) : "") + ++i;
    } else {
      return v;
    }
  }
}
