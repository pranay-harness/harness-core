package software.wings.app;

/**
 * Created by peeyushaggarwal on 1/11/17.
 * <p>
 * Copyright 2011-2013 Terracotta, Inc.
 * Copyright 2011-2013 Oracle America Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;

import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.DefaultCacheKeyGenerator;
import org.jsr107.ri.annotations.DefaultCacheResolverFactory;
import org.jsr107.ri.annotations.guice.CacheLookupUtil;
import org.jsr107.ri.annotations.guice.CachePutInterceptor;
import org.jsr107.ri.annotations.guice.CacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.guice.CacheRemoveEntryInterceptor;
import org.jsr107.ri.annotations.guice.CacheResultInterceptor;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.spi.CachingProvider;

/**
 * Standard cache module for binding all cache interceptors to their respective annotations. This module needs to be
 * part of the Guice injector instantiation to activate intercepting of the cache annotations. Every interceptor is
 * bound twice due to the fact that the annotations defining the joinpoints have retention type Method and Type.
 *
 * @author Michael Stachel
 * @version $Revision$
 */
public class CacheModule extends AbstractModule {
  private HazelcastInstance hazelcastInstance;
  private CacheManager cacheManager;

  public CacheModule(MainConfiguration mainConfiguration) {
    Config config = new XmlConfigBuilder().build();
    config.setInstanceName("wings-hazelcast");
    if (mainConfiguration.getHazelcast() != null) {
      if (mainConfiguration.getHazelcast().getAwsConfig() != null
          && mainConfiguration.getHazelcast().getAwsConfig().isEnabled()) {
        config.getNetworkConfig().getJoin().setAwsConfig(mainConfiguration.getHazelcast().getAwsConfig());
      }
      if (mainConfiguration.getHazelcast().getTcpIpConfig() != null
          && mainConfiguration.getHazelcast().getTcpIpConfig().isEnabled()) {
        config.getNetworkConfig().getJoin().setTcpIpConfig(mainConfiguration.getHazelcast().getTcpIpConfig());
      }
    }

    hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);

    Properties properties = new Properties();
    properties.setProperty(HazelcastCachingProvider.HAZELCAST_INSTANCE_NAME, "wings-hazelcast");
    CachingProvider provider = Caching.getCachingProvider();
    this.cacheManager =
        provider.getCacheManager(provider.getDefaultURI(), provider.getDefaultClassLoader(), properties);
  }

  public static <T, R> Supplier<R> bind(Function<T, R> fn, T val) {
    return () -> fn.apply(val);
  }

  @Override
  protected void configure() {
    bind(CacheKeyGenerator.class).to(DefaultCacheKeyGenerator.class);
    bind(CacheResolverFactory.class).toInstance(new DefaultCacheResolverFactory(cacheManager));
    bind(new TypeLiteral<CacheContextSource<MethodInvocation>>() {}).to(CacheLookupUtil.class);

    CachePutInterceptor cachePutInterceptor = new CachePutInterceptor();
    requestInjection(cachePutInterceptor);
    bindInterceptor(Matchers.annotatedWith(CachePut.class), Matchers.any(), cachePutInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(CachePut.class), cachePutInterceptor);

    CacheResultInterceptor cacheResultInterceptor = new CacheResultInterceptor();
    requestInjection(cacheResultInterceptor);
    bindInterceptor(Matchers.annotatedWith(CacheResult.class), Matchers.any(), cacheResultInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheResult.class), cacheResultInterceptor);

    CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor();
    requestInjection(cacheRemoveEntryInterceptor);
    bindInterceptor(Matchers.annotatedWith(CacheRemove.class), Matchers.any(), cacheRemoveEntryInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemove.class), cacheRemoveEntryInterceptor);

    CacheRemoveAllInterceptor cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor();
    requestInjection(cacheRemoveAllInterceptor);
    bindInterceptor(Matchers.annotatedWith(CacheRemoveAll.class), Matchers.any(), cacheRemoveAllInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemoveAll.class), cacheRemoveAllInterceptor);
  }

  public HazelcastInstance getHazelcastInstance() {
    return hazelcastInstance;
  }
}
