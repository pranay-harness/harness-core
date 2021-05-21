package io.harness.queryconverter;

import io.harness.queryconverter.dto.GridRequest;

import java.io.Serializable;
import java.util.List;
import lombok.NonNull;
import org.jooq.Record;
import org.jooq.impl.TableImpl;

public interface SQLConverter {
  List<? extends Serializable> convert(@NonNull GridRequest request) throws Exception;

  List<? extends Serializable> convert(@NonNull String tableName, GridRequest request) throws Exception;

  List<? extends Serializable> convert(TableImpl<? extends Record> jooqTable, GridRequest request) throws Exception;

  List<? extends Serializable> convert(String tableName, GridRequest request, Class<? extends Serializable> fetchInto)
      throws Exception;

  List<? extends Serializable> convert(TableImpl<? extends Record> jooqTable, GridRequest request,
      Class<? extends Serializable> fetchInto) throws Exception;
}
