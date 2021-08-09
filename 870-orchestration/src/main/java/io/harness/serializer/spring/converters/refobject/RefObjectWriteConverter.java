package io.harness.serializer.spring.converters.refobject;

import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.serializer.spring.ProtoWriteConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@Singleton
@WritingConverter
public class RefObjectWriteConverter extends ProtoWriteConverter<RefObject> {}
