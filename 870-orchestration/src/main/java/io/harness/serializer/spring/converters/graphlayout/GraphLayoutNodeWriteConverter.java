package io.harness.serializer.spring.converters.graphlayout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.contracts.plan.GraphLayoutNode;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class GraphLayoutNodeWriteConverter extends ProtoWriteConverter<GraphLayoutNode> {}
