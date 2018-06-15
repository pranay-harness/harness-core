package io.harness.cache;

public interface DistributedStore { <T extends Distributable> void upsert(T obj); }
