package it.uniroma3.mat.extendedset.intset;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;

public class ImmutableConciseSet
{  
  private final IntBuffer words;
  private final int size;
  
  private final ConciseSet mutableSet;

  public static ConciseSet union(List<ImmutableConciseSet> sets)
  {
    ConciseSet retVal = new ConciseSet();
    for (ImmutableConciseSet set : sets) {
      retVal = retVal.union(set.toMutableConciseSet());
    }
    return retVal;
  }

  public static ConciseSet intersection(List<ImmutableConciseSet> sets)
  {
    ConciseSet retVal = new ConciseSet();
    for (ImmutableConciseSet set : sets) {
      retVal = retVal.intersection(set.toMutableConciseSet());
    }
    return retVal;
  }

  public ImmutableConciseSet(ByteBuffer byteBuffer)
  {
    this.words = byteBuffer.asIntBuffer();
    this.mutableSet = toMutableConciseSet();
    this.size = byteBuffer.capacity() / 4;
  }

  public ImmutableConciseSet(ConciseSet conciseSet)
  {
    this.words = conciseSet == null || conciseSet.isEmpty() ? null : IntBuffer.wrap(conciseSet.getWords());
    this.mutableSet = conciseSet;
    this.size = conciseSet.size();
  }

  public ConciseSet toMutableConciseSet()
  {
    return mutableSet;
    /*if (isEmpty()) {
      return new ConciseSet();
    }
    return new ConciseSet(words.array(), false);*/
  }

  public byte[] toBytes()
  {
    ByteBuffer buf = ByteBuffer.wrap(new byte[words.remaining() * 4]);
    buf.asIntBuffer().put(words);
    return buf.array();
  }

  public int get(int i)
  {
    if (isEmpty() || i < 0) {
      throw new IndexOutOfBoundsException(Integer.toString(i));
    }

    return mutableSet.get(i);
  }

  public int size()
  {
    return size;
  }

  @Override
  public String toString()
  {
    return mutableSet.toString();
  }

  public ImmutableConciseSet clone()
  {
    return new ImmutableConciseSet(mutableSet.clone());
  }

  public int compareTo(ImmutableConciseSet other)
  {
    return words.compareTo(other.words);
  }

  private boolean isEmpty()
  {
    return words == null;
  }

  private class WordIterator<T> implements Iterator<T>
  {
    @Override
    public boolean hasNext()
    {
      return false;
    }

    @Override
    public T next()
    {
      return null;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
