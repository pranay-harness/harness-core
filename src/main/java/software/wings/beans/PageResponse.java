package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.jetty.util.LazyList;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * PageResponse bean class.
 *
 * @author Rishi
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> extends PageRequest<T> implements List<T> {
  private List<T> response;
  private long total;

  public PageResponse() {}

  public PageResponse(PageRequest<T> req) {
    super(req);
  }

  public List<T> getResponse() {
    return response;
  }

  public void setResponse(List<T> response) {
    this.response = LazyList.isEmpty(response) ? Collections.emptyList() : response;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  /**
   * returns number of page for the collection.
   *
   * @return page number.
   */
  public long getCurrentPage() {
    if (getPageSize() == 0) {
      return 0;
    }
    int pageno = getStart() / getPageSize();
    if (getStart() % getPageSize() == 0) {
      pageno++;
    }
    return pageno;
  }

  @Override
  public int size() {
    return response.size();
  }

  @Override
  public boolean isEmpty() {
    return response.isEmpty();
  }

  @Override
  public boolean contains(Object item) {
    return response.contains(item);
  }

  @Override
  public Iterator<T> iterator() {
    return response.iterator();
  }

  @Override
  public Object[] toArray() {
    return response.toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] array) {
    return response.toArray(array);
  }

  @Override
  public boolean add(T item) {
    return response.add(item);
  }

  @Override
  public void add(int index, T element) {
    response.add(index, element);
  }

  @Override
  public boolean remove(Object item) {
    return response.remove(item);
  }

  @Override
  public T remove(int index) {
    return response.remove(index);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    return response.containsAll(collection);
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    return response.addAll(collection);
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> collection) {
    return response.addAll(index, collection);
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    return response.removeAll(collection);
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    return response.retainAll(collection);
  }

  @Override
  public void replaceAll(UnaryOperator<T> operator) {
    response.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super T> comparator) {
    response.sort(comparator);
  }

  @Override
  public void clear() {
    response.clear();
  }

  @Override
  public T get(int index) {
    return response.get(index);
  }

  @Override
  public T set(int index, T element) {
    return response.set(index, element);
  }

  @Override
  public int indexOf(Object item) {
    return response.indexOf(item);
  }

  @Override
  public int lastIndexOf(Object item) {
    return response.lastIndexOf(item);
  }

  @Override
  public ListIterator<T> listIterator() {
    return response.listIterator();
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return response.listIterator(index);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return response.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<T> spliterator() {
    return response.spliterator();
  }

  @Override
  public boolean removeIf(Predicate<? super T> filter) {
    return response.removeIf(filter);
  }

  @Override
  public Stream<T> stream() {
    return response.stream();
  }

  @Override
  public Stream<T> parallelStream() {
    return response.parallelStream();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    response.forEach(action);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageResponse<?> that = (PageResponse<?>) o;
    return total == that.total && Objects.equal(response, that.response);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(response, total);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("response", response)
        .add("total", total)
        .add("currentPage", getCurrentPage())
        .toString();
  }
}
