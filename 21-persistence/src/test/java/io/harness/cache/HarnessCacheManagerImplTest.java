package io.harness.cache;

import static io.harness.cache.CacheBackend.REDIS;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.Collections;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

public class HarnessCacheManagerImplTest extends CategoryTest {
  @Captor private ArgumentCaptor<MutableConfiguration<String, Integer>> configCaptor;
  private static final String DISABLED_CACHE_NAME = "disabledCache";
  private HarnessCacheManager harnessCacheManager;
  private CacheManager cacheManager;

  @Before
  public void setup() {
    initMocks(this);
    cacheManager = mock(CacheManager.class);
    CacheConfig cacheConfig = CacheConfig.builder()
                                  .cacheBackend(REDIS)
                                  .cacheNamespace("test")
                                  .disabledCaches(Collections.singleton(DISABLED_CACHE_NAME))
                                  .build();
    this.harnessCacheManager = new HarnessCacheManagerImpl(cacheManager, cacheConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getCache_shouldReturnCache() {
    String cacheName = "testCache";
    when(cacheManager.getCache(cacheName, String.class, Object.class)).thenReturn(new NoOpCache<>());
    Cache<String, Object> cache = harnessCacheManager.getCache(
        cacheName, String.class, Object.class, AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
    assertThat(cache).isNotNull();
    verify(cacheManager, times(1)).getCache(cacheName, String.class, Object.class);
    verify(cacheManager, times(0)).createCache(any(), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getCache_shouldCreateCache() {
    String cacheName = "testCache";
    Factory<ExpiryPolicy> expiryPolicy = AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES);
    when(cacheManager.getCache(cacheName, String.class, Integer.class)).thenReturn(null);
    when(cacheManager.createCache(eq(cacheName), any(MutableConfiguration.class))).thenReturn(new NoOpCache<>());
    Cache<String, Integer> cache = harnessCacheManager.getCache(cacheName, String.class, Integer.class, expiryPolicy);
    assertThat(cache).isNotNull();
    verify(cacheManager, times(1)).getCache(cacheName, String.class, Integer.class);
    verify(cacheManager, times(1)).createCache(eq(cacheName), configCaptor.capture());

    MutableConfiguration<String, Integer> cacheConfiguration = configCaptor.getValue();

    assertThat(cacheConfiguration).isNotNull();
    assertThat(cacheConfiguration.getKeyType()).isEqualTo(String.class);
    assertThat(cacheConfiguration.getValueType()).isEqualTo(Integer.class);
    assertThat(cacheConfiguration.isStoreByValue()).isTrue();
    assertThat(cacheConfiguration.getExpiryPolicyFactory()).isEqualTo(expiryPolicy);
    assertThat(cacheConfiguration.isManagementEnabled()).isTrue();
    assertThat(cacheConfiguration.isStatisticsEnabled()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getCache_shouldReturnCacheAfterError_Hazelcast() {
    String cacheName = "testCache";
    Factory<ExpiryPolicy> expiryPolicy = AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES);
    when(cacheManager.getCache(cacheName, String.class, Integer.class)).thenReturn(null).thenReturn(new NoOpCache<>());
    when(cacheManager.createCache(eq(cacheName), any(MutableConfiguration.class)))
        .thenThrow(new CacheException("A cache named " + cacheName + " already exists."));
    Cache<String, Integer> cache = harnessCacheManager.getCache(cacheName, String.class, Integer.class, expiryPolicy);
    assertThat(cache).isNotNull();
    verify(cacheManager, times(2)).getCache(cacheName, String.class, Integer.class);
    verify(cacheManager, times(1)).createCache(eq(cacheName), any(MutableConfiguration.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getCache_shouldReturnCacheAfterError_RedisAndCaffeine() {
    String cacheName = "testCache";
    Factory<ExpiryPolicy> expiryPolicy = AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES);
    when(cacheManager.getCache(cacheName, String.class, Integer.class)).thenReturn(null).thenReturn(new NoOpCache<>());
    when(cacheManager.createCache(eq(cacheName), any(MutableConfiguration.class)))
        .thenThrow(new CacheException("Cache " + cacheName + " already exists"));
    Cache<String, Integer> cache = harnessCacheManager.getCache(cacheName, String.class, Integer.class, expiryPolicy);
    assertThat(cache).isNotNull();
    verify(cacheManager, times(2)).getCache(cacheName, String.class, Integer.class);
    verify(cacheManager, times(1)).createCache(eq(cacheName), any(MutableConfiguration.class));
  }

  @Test(expected = CacheException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getCache_shouldThrowError() {
    String cacheName = "testCache";
    Factory<ExpiryPolicy> expiryPolicy = AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES);
    when(cacheManager.getCache(cacheName, String.class, Integer.class)).thenReturn(null).thenReturn(new NoOpCache<>());
    when(cacheManager.createCache(eq(cacheName), any(MutableConfiguration.class)))
        .thenThrow(new CacheException("Connection failed"));
    Cache<String, Integer> cache = harnessCacheManager.getCache(cacheName, String.class, Integer.class, expiryPolicy);
    assertThat(cache).isNotNull();
    verify(cacheManager, times(1)).getCache(cacheName, String.class, Integer.class);
    verify(cacheManager, times(1)).createCache(eq(cacheName), any(MutableConfiguration.class));
  }
}
