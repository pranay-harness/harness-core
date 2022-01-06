/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cache;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Cache;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by peeyushaggarwal on 8/29/16.
 */
@Cache
public class CachingTest extends WingsBaseTest {
  @Inject private CacheableService cacheableService;

  /**
   * Should cache repeated calls.
   */
  @Cache
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCacheRepeatedCalls() {
    assertThat(cacheableService.getCacheableObject(1, 1)).extracting(CacheableObject::getX).isEqualTo(1);
    assertThat(cacheableService.getCallCount()).isEqualTo(1);
    assertThat(cacheableService.getCacheableObject(1, 2)).extracting(CacheableObject::getX).isEqualTo(1);
    assertThat(cacheableService.getCallCount()).isEqualTo(1);

    assertThat(cacheableService.getCacheableObject(2, 1)).extracting(CacheableObject::getX).isEqualTo(2);
    assertThat(cacheableService.getCallCount()).isEqualTo(2);
    assertThat(cacheableService.getCacheableObject(1, 2)).extracting(CacheableObject::getX).isEqualTo(1);
    assertThat(cacheableService.getCallCount()).isEqualTo(2);
  }

  /**
   * The type Cacheable object.
   */
  public static class CacheableObject {
    /**
     * The X.
     */
    int x;

    /**
     * Getter for property 'x'.
     *
     * @return Value for property 'x'.
     */
    public int getX() {
      return x;
    }

    /**
     * Setter for property 'x'.
     *
     * @param x Value to set for property 'x'.
     */
    public void setX(int x) {
      this.x = x;
    }
  }

  /**
   * The type Cacheable service.
   */
  public static class CacheableService {
    private int callCount;

    /**
     * Gets cacheable object.
     *
     * @param x the x
     * @param y the y
     * @return the cacheable object
     */
    @CacheResult(
        cacheName = "TestCache", exceptionCacheName = "ExceptionTestCache", cachedExceptions = WingsException.class)
    CacheableObject
    getCacheableObject(@CacheKey int x, int y) {
      CacheableObject toReturn = new CacheableObject();
      toReturn.setX(x);
      callCount++;
      return toReturn;
    }

    /**
     * Gets call count.
     *
     * @return the call count
     */
    public int getCallCount() {
      return callCount;
    }
  }
}
