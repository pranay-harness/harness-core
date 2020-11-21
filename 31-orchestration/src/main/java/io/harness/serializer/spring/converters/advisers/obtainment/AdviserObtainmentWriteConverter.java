package io.harness.serializer.spring.converters.advisers.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class AdviserObtainmentWriteConverter extends ProtoWriteConverter<AdviserObtainment> {}
