package io.harness.cvng.core.entities.cvnglogs;

import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.persistence.CreatedAtAware;

import java.util.Comparator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CVNGLogRecord implements CreatedAtAware {
  private long createdAt;
  public abstract CVNGLogDTO toCVNGLogDTO();

  public static class CVNGLogRecordComparator implements Comparator<CVNGLogRecord> {
    @Override
    public int compare(CVNGLogRecord o1, CVNGLogRecord o2) {
      Long c1 = o1.getCreatedAt();
      Long c2 = o2.getCreatedAt();
      return c2.compareTo(c1);
    }
  }
}
