package it.uniroma3.mat.extendedset.intset;

import java.util.NoSuchElementException;

public final class EmptyIntIterator implements IntSet.IntIterator
{
  private static final EmptyIntIterator INSTANCE = new EmptyIntIterator();

  public static EmptyIntIterator instance()
  {
    return INSTANCE;
  }

  private EmptyIntIterator() {}

  @Override
  public boolean hasNext()
  {
    return false;
  }

  @Override
  public int next()
  {
    throw new NoSuchElementException();
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void skipAllBefore(int element)
  {
    // nothing to skip
  }

  @Override
  public IntSet.IntIterator clone()
  {
    return new EmptyIntIterator();
  }
}
