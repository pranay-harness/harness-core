package software.wings.dl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/23/16.
 *
 * @param <E> the element type
 */
public class WingsDeque<E> extends ArrayDeque<E> implements List<E> {
  private static final long serialVersionUID = -3543720415386379667L;

  /**
   * Instantiates a new wings deque.
   */
  public WingsDeque() {}

  /* (non-Javadoc)
   * @see java.util.List#addAll(int, java.util.Collection)
   */
  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#get(int)
   */
  @Override
  public E get(int index) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#set(int, java.lang.Object)
   */
  @Override
  public E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#add(int, java.lang.Object)
   */
  @Override
  public void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#remove(int)
   */
  @Override
  public E remove(int index) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#indexOf(java.lang.Object)
   */
  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#listIterator()
   */
  @Override
  public ListIterator<E> listIterator() {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#listIterator(int)
   */
  @Override
  public ListIterator<E> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.util.List#subList(int, int)
   */
  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }
}
