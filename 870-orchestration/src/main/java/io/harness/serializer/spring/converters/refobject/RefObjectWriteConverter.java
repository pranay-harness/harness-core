package io.harness.serializer.spring.converters.refobject;

import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.refobjects.RefObject;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@Singleton
@WritingConverter
public class RefObjectWriteConverter extends ProtoWriteConverter<RefObject> {}
