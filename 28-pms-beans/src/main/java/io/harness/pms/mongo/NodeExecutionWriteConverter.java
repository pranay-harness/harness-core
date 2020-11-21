package io.harness.pms.mongo;

import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.execution.NodeExecution;

import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class NodeExecutionWriteConverter extends ProtoWriteConverter<NodeExecution> {}
