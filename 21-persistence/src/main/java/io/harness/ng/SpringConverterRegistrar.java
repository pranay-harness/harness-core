package io.harness.ng;

import org.springframework.core.convert.converter.Converter;

import java.util.Set;

public interface SpringConverterRegistrar { Set<Converter> registerConverters(Set<Converter> converters); }
