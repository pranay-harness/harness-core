package io.harness.serializer.spring.converters.timeout.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.spring.ProtoWriteConverter;
import io.harness.timeout.contracts.TimeoutObtainment;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class TimeoutObtainmentWriteConverter extends ProtoWriteConverter<TimeoutObtainment> {}
