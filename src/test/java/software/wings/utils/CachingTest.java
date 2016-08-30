package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 8/29/16.
 */
public class CachingTest extends WingsBaseTest {
  @Inject private CacheableService cacheableService;

  @Test
  public void shouldCacheRepeatedCalls() {
    assertThat(cacheableService.getCacheableObject(1, 1)).extracting(CacheableObject::getX).contains(1);
    assertThat(cacheableService.getCallCount()).isEqualTo(1);
    assertThat(cacheableService.getCacheableObject(1, 2)).extracting(CacheableObject::getX).contains(1);
    assertThat(cacheableService.getCallCount()).isEqualTo(1);
  }

  @Test
  public void shouldNotCacheWhenKeyIsDifferent() {
    assertThat(cacheableService.getCacheableObject(1, 1)).extracting(CacheableObject::getX).contains(1);
    assertThat(cacheableService.getCallCount()).isEqualTo(1);
    assertThat(cacheableService.getCacheableObject(2, 1)).extracting(CacheableObject::getX).contains(2);
    assertThat(cacheableService.getCallCount()).isEqualTo(2);
  }

  public static class CacheableObject {
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

  public static class CacheableService {
    private int callCount = 0;

    @CacheResult
    public CacheableObject getCacheableObject(@CacheKey int x, int y) {
      CacheableObject toReturn = new CacheableObject();
      toReturn.setX(x);
      callCount++;
      return toReturn;
    }

    public int getCallCount() {
      return callCount;
    }
  }
}
