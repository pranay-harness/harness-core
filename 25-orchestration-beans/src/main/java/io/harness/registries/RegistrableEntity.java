package io.harness.registries;

public interface RegistrableEntity<K extends RegistryKey> { K getType(); }
