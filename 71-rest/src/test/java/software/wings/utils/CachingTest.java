package software.wings.utils;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.cache.HarnessCacheManager;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Cache;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

/**
 * Created by peeyushaggarwal on 8/29/16.
 */
@Cache
public class CachingTest extends WingsBaseTest {
  @Inject private CacheableService cacheableService;
  @Inject private HarnessCacheManager harnessCacheManager;

  /**
   * Should cache repeated calls.
   */
  @Cache
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCacheRepeatedCalls() {
    harnessCacheManager.getCache(
        "TestCache", Integer.class, Object.class, AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
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
    @CacheResult(cacheName = "TestCache")
    public CacheableObject getCacheableObject(@CacheKey int x, int y) {
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
