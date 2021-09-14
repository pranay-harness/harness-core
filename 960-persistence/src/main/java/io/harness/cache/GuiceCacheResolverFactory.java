/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cache;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.CacheNotFoundException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import lombok.extern.slf4j.Slf4j;
import org.jsr107.ri.annotations.DefaultCacheResolver;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GuiceCacheResolverFactory implements CacheResolverFactory {
  private Map<String, Cache<?, ?>> caches;

  @Inject
  public GuiceCacheResolverFactory(Map<String, Cache<?, ?>> caches) {
    this.caches = caches;
  }

  @Override
  public CacheResolver getCacheResolver(CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
    final String cacheName = cacheMethodDetails.getCacheName();
    Cache<?, ?> cache = Optional.ofNullable(caches.get(cacheName)).<CacheNotFoundException>orElseThrow(() -> {
      throw new CacheNotFoundException(String.format("The cache %s is not registered with guice", cacheName), USER);
    });
    return new DefaultCacheResolver(cache);
  }

  @Override
  public CacheResolver getExceptionCacheResolver(CacheMethodDetails<CacheResult> cacheMethodDetails) {
    final CacheResult cacheResultAnnotation = cacheMethodDetails.getCacheAnnotation();
    final String exceptionCacheName = cacheResultAnnotation.exceptionCacheName();
    if (isEmpty(exceptionCacheName)) {
      throw new IllegalArgumentException("Can only be called when CacheResult.exceptionCacheName() is specified");
    }
    Cache<?, ?> cache = Optional.ofNullable(caches.get(exceptionCacheName)).<CacheNotFoundException>orElseThrow(() -> {
      throw new CacheNotFoundException(
          String.format("The exception cache %s is not registered with guice", exceptionCacheName), USER);
    });
    return new DefaultCacheResolver(cache);
  }
}
