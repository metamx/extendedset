/* 
 * (c) 2010 Alessandro Colantonio
 * <mailto:colanton@mat.uniroma3.it>
 * <http://ricerca.mat.uniroma3.it/users/colanton>
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package it.uniroma3.mat.extendedset.intset;


import it.uniroma3.mat.extendedset.utilities.BitCount;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Formatter;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * This is CONCISE: COmpressed 'N' Composable Integer SEt.
 * <p/>
 * This class is an instance of {@link IntSet} internally represented by
 * compressed bitmaps though a RLE (Run-Length Encoding) compression algorithm.
 * See <a
 * href="http://ricerca.mat.uniroma3.it/users/colanton/docs/concise.pdf">http
 * ://ricerca.mat.uniroma3.it/users/colanton/docs/concise.pdf</a> for more
 * details.
 * <p/>
 * Notice that the iterator by {@link #iterator()} is <i>fail-fast</i>,
 * similar to most {@link Collection}-derived classes. If the set is
 * structurally modified at any time after the iterator is created, the iterator
 * will throw a {@link ConcurrentModificationException}. Thus, in the face of
 * concurrent modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined time in the
 * future. The iterator throws a {@link ConcurrentModificationException} on a
 * best-effort basis. Therefore, it would be wrong to write a program that
 * depended on this exception for its correctness: <i>the fail-fast behavior of
 * iterators should be used only to detect bugs.</i>
 *
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class ImmutableConciseSet extends AbstractIntSet implements java.io.Serializable
{
  /**
   * This is the compressed bitmap, that is a collection of words. For each
   * word:
   * <ul>
   * <li> <tt>1* (0x80000000)</tt> means that it is a 31-bit <i>literal</i>.
   * <li> <tt>00* (0x00000000)</tt> indicates a <i>sequence</i> made up of at
   * most one set bit in the first 31 bits, and followed by blocks of 31 0's.
   * The following 5 bits (<tt>00xxxxx*</tt>) indicates which is the set bit (
   * <tt>00000</tt> = no set bit, <tt>00001</tt> = LSB, <tt>11111</tt> = MSB),
   * while the remaining 25 bits indicate the number of following 0's blocks.
   * <li> <tt>01* (0x40000000)</tt> indicates a <i>sequence</i> made up of at
   * most one <i>un</i>set bit in the first 31 bits, and followed by blocks of
   * 31 1's. (see the <tt>00*</tt> case above).
   * </ul>
   * <p/>
   * Note that literal words 0xFFFFFFFF and 0x80000000 are allowed, thus
   * zero-length sequences (i.e., such that getSequenceCount() == 0) cannot
   * exists.
   */
  private final IntBuffer words;

  /**
   * Most significant set bit within the uncompressed bit string.
   */
  private transient int last;

  /**
   * Cached cardinality of the bit-set. Defined for efficient {@link #size()}
   * calls. When -1, the cache is invalid.
   */
  private transient int size;

  /**
   * Index of the last word in {@link #words}
   */
  private transient int lastWordIndex;

  /**
   * <code>true</code> if the class must simulate the behavior of WAH
   */
  private final boolean simulateWAH;

  /**
   * Maximum number of representable bits within a literal
   */
  private final static int MAX_LITERAL_LENGHT = 31;

  /**
   * Literal that represents all bits set to 1 (and MSB = 1)
   */
  private final static int ALL_ONES_LITERAL = 0xFFFFFFFF;

  /**
   * Literal that represents all bits set to 0 (and MSB = 1)
   */
  private final static int ALL_ZEROS_LITERAL = 0x80000000;

  /**
   * All bits set to 1 and MSB = 0
   */
  private final static int ALL_ONES_WITHOUT_MSB = 0x7FFFFFFF;

  /**
   * Sequence bit
   */
  private final static int SEQUENCE_BIT = 0x40000000;

  public ImmutableConciseSet(ByteBuffer byteBuffer)
  {
    this.words = byteBuffer.asIntBuffer();
    this.lastWordIndex = (this.words.capacity() / 4) - 1;
    this.size = -1;
    updateLast();
    this.simulateWAH = false;
  }

  public ImmutableConciseSet(ConciseSet conciseSet)
  {
    boolean nullSet = conciseSet == null || conciseSet.isEmpty();
    this.words = nullSet ? null : IntBuffer.wrap(conciseSet.getWords());
    this.lastWordIndex = nullSet ? -1 : conciseSet.getLastWordIndex();
    this.size = conciseSet.size();
    this.last = conciseSet.last();
    this.simulateWAH = false;
  }

  public ImmutableConciseSet()
  {
		this.words = null;
		this.last = -1;
		this.size = 0;
		this.lastWordIndex = -1;
    this.simulateWAH = false;
  }

  private ImmutableConciseSet(IntBuffer words)
  {
    this.words = words;
    this.simulateWAH = false;
  }

  public ConciseSet toMutableConciseSet()
  {
    ConciseSet ret = new ConciseSet();
    ret.addAll(this);
    return ret;
  }

  public byte[] toBytes()
  {
    return toMutableConciseSet().toByteBuffer().array();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ImmutableConciseSet clone()
  {
    if (isEmpty()) {
      return empty();
    }
    ImmutableConciseSet res = new ImmutableConciseSet(words.duplicate());
    res.last = last;
    res.lastWordIndex = lastWordIndex;
    res.size = size;

    return res;
  }

  /**
   * Calculates the modulus division by 31 in a faster way than using <code>n % 31</code>
   * <p/>
   * This method of finding modulus division by an integer that is one less
   * than a power of 2 takes at most <tt>O(lg(32))</tt> time. The number of operations
   * is at most <tt>12 + 9 * ceil(lg(32))</tt>.
   * <p/>
   * See <a
   * href="http://graphics.stanford.edu/~seander/bithacks.html">http://graphics.stanford.edu/~seander/bithacks.html</a>
   *
   * @param n number to divide
   *
   * @return <code>n % 31</code>
   */
  private static int maxLiteralLengthModulus(int n)
  {
    int m = (n & 0xC1F07C1F) + ((n >>> 5) & 0xC1F07C1F);
    m = (m >>> 15) + (m & 0x00007FFF);
    if (m <= 31) {
      return m == 31 ? 0 : m;
    }
    m = (m >>> 5) + (m & 0x0000001F);
    if (m <= 31) {
      return m == 31 ? 0 : m;
    }
    m = (m >>> 5) + (m & 0x0000001F);
    if (m <= 31) {
      return m == 31 ? 0 : m;
    }
    m = (m >>> 5) + (m & 0x0000001F);
    if (m <= 31) {
      return m == 31 ? 0 : m;
    }
    m = (m >>> 5) + (m & 0x0000001F);
    if (m <= 31) {
      return m == 31 ? 0 : m;
    }
    m = (m >>> 5) + (m & 0x0000001F);
    return m == 31 ? 0 : m;
  }

  /**
   * Calculates the multiplication by 31 in a faster way than using <code>n * 31</code>
   *
   * @param n number to multiply
   *
   * @return <code>n * 31</code>
   */
  private static int maxLiteralLengthMultiplication(int n)
  {
    return (n << 5) - n;
  }

  /**
   * Calculates the division by 31
   *
   * @param n number to divide
   *
   * @return <code>n / 31</code>
   */
  private static int maxLiteralLengthDivision(int n)
  {
    return n / 31;
  }

  /**
   * Checks whether a word is a literal one
   *
   * @param word word to check
   *
   * @return <code>true</code> if the given word is a literal word
   */
  private static boolean isLiteral(int word)
  {
    // "word" must be 1*
    // NOTE: this is faster than "return (word & 0x80000000) == 0x80000000"
    return (word & 0x80000000) != 0;
  }

  /**
   * Checks whether a word contains a sequence of 1's
   *
   * @param word word to check
   *
   * @return <code>true</code> if the given word is a sequence of 1's
   */
  private static boolean isOneSequence(int word)
  {
    // "word" must be 01*
    return (word & 0xC0000000) == SEQUENCE_BIT;
  }

  /**
   * Checks whether a word contains a sequence of 0's
   *
   * @param word word to check
   *
   * @return <code>true</code> if the given word is a sequence of 0's
   */
  private static boolean isZeroSequence(int word)
  {
    // "word" must be 00*
    return (word & 0xC0000000) == 0;
  }

  /**
   * Checks whether a word contains a sequence of 0's with no set bit, or 1's
   * with no unset bit.
   * <p/>
   * <b>NOTE:</b> when {@link #simulateWAH} is <code>true</code>, it is
   * equivalent to (and as fast as) <code>!</code>{@link #isLiteral(int)}
   *
   * @param word word to check
   *
   * @return <code>true</code> if the given word is a sequence of 0's or 1's
   *         but with no (un)set bit
   */
  private static boolean isSequenceWithNoBits(int word)
  {
    // "word" must be 0?00000*
    return (word & 0xBE000000) == 0x00000000;
  }

  /**
   * Gets the number of blocks of 1's or 0's stored in a sequence word
   *
   * @param word word to check
   *
   * @return the number of blocks that follow the first block of 31 bits
   */
  private static int getSequenceCount(int word)
  {
    // get the 25 LSB bits
    return word & 0x01FFFFFF;
  }

  /**
   * Clears the (un)set bit in a sequence
   *
   * @param word word to check
   *
   * @return the sequence corresponding to the given sequence and with no
   *         (un)set bits
   */
  private static int getSequenceWithNoBits(int word)
  {
    // clear 29 to 25 LSB bits
    return (word & 0xC1FFFFFF);
  }

  /**
   * Gets the literal word that represents the first 31 bits of the given the
   * word (i.e. the first block of a sequence word, or the bits of a literal word).
   * <p/>
   * If the word is a literal, it returns the unmodified word. In case of a
   * sequence, it returns a literal that represents the first 31 bits of the
   * given sequence word.
   *
   * @param word word to check
   *
   * @return the literal contained within the given word, <i>with the most
   *         significant bit set to 1</i>.
   */
  private /*static*/ int getLiteral(int word)
  {
    if (isLiteral(word)) {
      return word;
    }

    if (simulateWAH) {
      return isZeroSequence(word) ? ALL_ZEROS_LITERAL : ALL_ONES_LITERAL;
    }

    // get bits from 30 to 26 and use them to set the corresponding bit
    // NOTE: "1 << (word >>> 25)" and "1 << ((word >>> 25) & 0x0000001F)" are equivalent
    // NOTE: ">>> 1" is required since 00000 represents no bits and 00001 the LSB bit set
    int literal = (1 << (word >>> 25)) >>> 1;
    return isZeroSequence(word)
           ? (ALL_ZEROS_LITERAL | literal)
           : (ALL_ONES_LITERAL & ~literal);
  }

  /**
   * Gets the position of the flipped bit within a sequence word. If the
   * sequence has no set/unset bit, returns -1.
   * <p/>
   * Note that the parameter <i>must</i> a sequence word, otherwise the
   * result is meaningless.
   *
   * @param word sequence word to check
   *
   * @return the position of the set bit, from 0 to 31. If the sequence has no
   *         set/unset bit, returns -1.
   */
  private static int getFlippedBit(int word)
  {
    // get bits from 30 to 26
    // NOTE: "-1" is required since 00000 represents no bits and 00001 the LSB bit set
    return ((word >>> 25) & 0x0000001F) - 1;
  }

  /**
   * Gets the number of set bits within the literal word
   *
   * @param word literal word
   *
   * @return the number of set bits within the literal word
   */
  private static int getLiteralBitCount(int word)
  {
    return BitCount.count(getLiteralBits(word));
  }

  /**
   * Gets the bits contained within the literal word
   *
   * @param word literal word
   *
   * @return the literal word with the most significant bit cleared
   */
  private static int getLiteralBits(int word)
  {
    return ALL_ONES_WITHOUT_MSB & word;
  }

  /**
   * Iterates over words, from the rightmost (LSB) to the leftmost (MSB).
   * <p/>
   * When {@link ImmutableConciseSet#simulateWAH} is <code>false</code>, mixed
   * sequences are "broken" into a literal (i.e., the first block is coded
   * with a literal in {@link #word}) and a "pure" sequence (i.e., the
   * remaining blocks are coded with a sequence with no bits in {@link #word})
   */
  private class WordIterator
  {
    /**
     * copy of the current word
     */
    int word;

    /**
     * current word index
     */
    int index;

    /**
     * <code>true</code> if {@link #word} is a literal
     */
    boolean isLiteral;

    /**
     * number of blocks in the current word (1 for literals, > 1 for sequences)
     */
    int count;

    /**
     * Initialize data
     */
    WordIterator()
    {
      isLiteral = false;
      index = -1;
      prepareNext();
    }

    /**
     * Prepare the next value for {@link #word} after skipping a given
     * number of 31-bit blocks in the current sequence.
     * <p/>
     * <b>NOTE:</b> it works only when the current word is within a
     * sequence, namely a literal cannot be skipped. Moreover, the number of
     * blocks to skip must be less than the remaining blocks in the current
     * sequence.
     *
     * @param c number of 31-bit "blocks" to skip
     *
     * @return <code>false</code> if the next word does not exists
     */
    boolean prepareNext(int c)
    {
      assert c <= count;
      count -= c;
      if (count == 0) {
        return prepareNext();
      }
      return true;
    }

    /**
     * Prepare the next value for {@link #word}
     *
     * @return <code>false</code> if the next word does not exists
     */
    boolean prepareNext()
    {
      if (!simulateWAH && isLiteral && count > 1) {
        count--;
        isLiteral = false;
        word = getSequenceWithNoBits(words.get(index)) - 1;
        return true;
      }

      index++;
      if (index > lastWordIndex) {
        return false;
      }
      word = words.get(index);
      isLiteral = isLiteral(word);
      if (!isLiteral) {
        count = getSequenceCount(word) + 1;
        if (!simulateWAH && !isSequenceWithNoBits(word)) {
          isLiteral = true;
          int bit = (1 << (word >>> 25)) >>> 1;
          word = isZeroSequence(word)
                 ? (ALL_ZEROS_LITERAL | bit)
                 : (ALL_ONES_LITERAL & ~bit);
        }
      } else {
        count = 1;
      }
      return true;
    }

    /**
     * @return the literal word corresponding to each block contained in the
     *         current sequence word. Not to be used with literal words!
     */
    int toLiteral()
    {
      assert !isLiteral;
      return ALL_ZEROS_LITERAL | ((word << 1) >> MAX_LITERAL_LENGHT);
    }
  }

  /**
	 * Recalculate a fresh value for {@link ImmutableConciseSet#last}
	 */
	private void updateLast() {
		last = 0;
		for (int i = 0; i <= lastWordIndex; i++) {
			int w = words.get(i);
			if (isLiteral(w))
				last += MAX_LITERAL_LENGHT;
			else
				last += maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
		}

		int w = words.get(lastWordIndex);
		if (isLiteral(w))
			last -= Integer.numberOfLeadingZeros(getLiteralBits(w));
		else
			last--;
	}

  /**
   * {@inheritDoc}
   */
  @Override
  public int intersectionSize(IntSet o)
  {
    // special cases
    if (isEmpty() || o == null || o.isEmpty()) {
      return 0;
    }
    if (this == o) {
      return size();
    }

    final ImmutableConciseSet other = convert(o);

    // check whether the first operator starts with a sequence that
    // completely "covers" the second operator
    if (isSequenceWithNoBits(this.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(this.words.get(0)) + 1) > other.last) {
      if (isZeroSequence(this.words.get(0))) {
        return 0;
      }
      return other.size();
    }
    if (isSequenceWithNoBits(other.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(other.words.get(0)) + 1) > this.last) {
      if (isZeroSequence(other.words.get(0))) {
        return 0;
      }
      return this.size();
    }

    int res = 0;

    // scan "this" and "other"
    WordIterator thisItr = new WordIterator();
    WordIterator otherItr = other.new WordIterator();
    while (true) {
      if (!thisItr.isLiteral) {
        if (!otherItr.isLiteral) {
          int minCount = Math.min(thisItr.count, otherItr.count);
          if ((SEQUENCE_BIT & thisItr.word & otherItr.word) != 0) {
            res += maxLiteralLengthMultiplication(minCount);
          }
          if (!thisItr.prepareNext(minCount) | !otherItr.prepareNext(minCount)) // NOT ||
          {
            break;
          }
        } else {
          res += getLiteralBitCount(thisItr.toLiteral() & otherItr.word);
          thisItr.word--;
          if (!thisItr.prepareNext(1) | !otherItr.prepareNext()) // do NOT use "||"
          {
            break;
          }
        }
      } else if (!otherItr.isLiteral) {
        res += getLiteralBitCount(thisItr.word & otherItr.toLiteral());
        otherItr.word--;
        if (!thisItr.prepareNext() | !otherItr.prepareNext(1)) // do NOT use  "||"
        {
          break;
        }
      } else {
        res += getLiteralBitCount(thisItr.word & otherItr.word);
        if (!thisItr.prepareNext() | !otherItr.prepareNext()) // do NOT use  "||"
        {
          break;
        }
      }
    }

    return res;
  }

  /**
   * {@inheritDoc}
   */
  public IntBuffer getBuffer()
  {
    return words;
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public int get(int i)
  {
    if (i < 0) {
      throw new IndexOutOfBoundsException();
    }

    // initialize data
    int firstSetBitInWord = 0;
    int position = i;
    int setBitsInCurrentWord = 0;
    for (int j = 0; j <= lastWordIndex; j++) {
      int w = words.get(j);
      if (isLiteral(w)) {
        // number of bits in the current word
        setBitsInCurrentWord = getLiteralBitCount(w);

        // check if the desired bit is in the current word
        if (position < setBitsInCurrentWord) {
          int currSetBitInWord = -1;
          for (; position >= 0; position--) {
            currSetBitInWord = Integer.numberOfTrailingZeros(w & (0xFFFFFFFF << (currSetBitInWord + 1)));
          }
          return firstSetBitInWord + currSetBitInWord;
        }

        // skip the 31-bit block
        firstSetBitInWord += MAX_LITERAL_LENGHT;
      } else {
        // number of involved bits (31 * blocks)
        int sequenceLength = maxLiteralLengthMultiplication(getSequenceCount(w) + 1);

        // check the sequence type
        if (isOneSequence(w)) {
          if (simulateWAH || isSequenceWithNoBits(w)) {
            setBitsInCurrentWord = sequenceLength;
            if (position < setBitsInCurrentWord) {
              return firstSetBitInWord + position;
            }
          } else {
            setBitsInCurrentWord = sequenceLength - 1;
            if (position < setBitsInCurrentWord)
            // check whether the desired set bit is after the
            // flipped bit (or after the first block)
            {
              return firstSetBitInWord + position + (position < getFlippedBit(w) ? 0 : 1);
            }
          }
        } else {
          if (simulateWAH || isSequenceWithNoBits(w)) {
            setBitsInCurrentWord = 0;
          } else {
            setBitsInCurrentWord = 1;
            if (position == 0) {
              return firstSetBitInWord + getFlippedBit(w);
            }
          }
        }

        // skip the 31-bit blocks
        firstSetBitInWord += sequenceLength;
      }

      // update the number of found set bits
      position -= setBitsInCurrentWord;
    }

    throw new IndexOutOfBoundsException(Integer.toString(i));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int indexOf(int e)
  {
    if (e < 0) {
      throw new IllegalArgumentException("positive integer expected: " + Integer.toString(e));
    }
    if (isEmpty()) {
      return -1;
    }

    // returned value
    int index = 0;

    int blockIndex = maxLiteralLengthDivision(e);
    int bitPosition = maxLiteralLengthModulus(e);
    for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
      int w = words.get(i);
      if (isLiteral(w)) {
        // check if the current literal word is the "right" one
        if (blockIndex == 0) {
          if ((w & (1 << bitPosition)) == 0) {
            return -1;
          }
          return index + BitCount.count(w & ~(0xFFFFFFFF << bitPosition));
        }
        blockIndex--;
        index += getLiteralBitCount(w);
      } else {
        if (simulateWAH) {
          if (isOneSequence(w) && blockIndex <= getSequenceCount(w)) {
            return index + maxLiteralLengthMultiplication(blockIndex) + bitPosition;
          }
        } else {
          // if we are at the beginning of a sequence, and it is
          // a set bit, the bit already exists
          if (blockIndex == 0) {
            int l = getLiteral(w);
            if ((l & (1 << bitPosition)) == 0) {
              return -1;
            }
            return index + BitCount.count(l & ~(0xFFFFFFFF << bitPosition));
          }

          // if we are in the middle of a sequence of 1's, the bit already exist
          if (blockIndex > 0
              && blockIndex <= getSequenceCount(w)
              && isOneSequence(w)) {
            return index + maxLiteralLengthMultiplication(blockIndex) + bitPosition - (isSequenceWithNoBits(w) ? 0 : 1);
          }
        }

        // next word
        int blocks = getSequenceCount(w) + 1;
        blockIndex -= blocks;
        if (isZeroSequence(w)) {
          if (!simulateWAH && !isSequenceWithNoBits(w)) {
            index++;
          }
        } else {
          index += maxLiteralLengthMultiplication(blocks);
          if (!simulateWAH && !isSequenceWithNoBits(w)) {
            index--;
          }
        }
      }
    }

    // not found
    return -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConciseSet intersection(IntSet other)
  {
    return toMutableConciseSet().intersection(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConciseSet union(IntSet other)
  {
    return toMutableConciseSet().union(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConciseSet difference(IntSet other)
  {
    return toMutableConciseSet().difference(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConciseSet symmetricDifference(IntSet other)
  {
    return toMutableConciseSet().symmetricDifference(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConciseSet complemented()
  {
    return toMutableConciseSet().complemented();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void complement()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Iterator over the bits of a single literal/fill word
   */
  private interface WordExpander
  {
    public boolean hasNext();

    public boolean hasPrevious();

    public int next();

    public int previous();

    public void skipAllAfter(int i);

    public void skipAllBefore(int i);

    public void reset(int offset, int word, boolean fromBeginning);
  }

  /**
   * Iterator over the bits of literal and zero-fill words
   */
  private class LiteralAndZeroFillExpander implements WordExpander
  {
    final int[] buffer = new int[MAX_LITERAL_LENGHT];
    int len = 0;
    int current = 0;

    @Override
    public boolean hasNext()
    {
      return current < len;
    }

    @Override
    public boolean hasPrevious()
    {
      return current > 0;
    }

    @Override
    public int next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return buffer[current++];
    }

    @Override
    public int previous()
    {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      return buffer[--current];
    }

    @Override
    public void skipAllAfter(int i)
    {
      while (hasPrevious() && buffer[current - 1] > i) {
        current--;
      }
    }

    @Override
    public void skipAllBefore(int i)
    {
      while (hasNext() && buffer[current] < i) {
        current++;
      }
    }

    @Override
    public void reset(int offset, int word, boolean fromBeginning)
    {
      if (isLiteral(word)) {
        len = 0;
        for (int i = 0; i < MAX_LITERAL_LENGHT; i++) {
          if ((word & (1 << i)) != 0) {
            buffer[len++] = offset + i;
          }
        }
        current = fromBeginning ? 0 : len;
      } else {
        if (isZeroSequence(word)) {
          if (simulateWAH || isSequenceWithNoBits(word)) {
            len = 0;
            current = 0;
          } else {
            len = 1;
            buffer[0] = offset + ((0x3FFFFFFF & word) >>> 25) - 1;
            current = fromBeginning ? 0 : 1;
          }
        } else {
          throw new RuntimeException("sequence of ones!");
        }
      }
    }
  }

  /**
   * Iterator over the bits of one-fill words
   */
  private class OneFillExpander implements WordExpander
  {
    int firstInt = 1;
    int lastInt = -1;
    int current = 0;
    int exception = -1;

    @Override
    public boolean hasNext()
    {
      return current < lastInt;
    }

    @Override
    public boolean hasPrevious()
    {
      return current > firstInt;
    }

    @Override
    public int next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      current++;
      if (!simulateWAH && current == exception) {
        current++;
      }
      return current;
    }

    @Override
    public int previous()
    {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      current--;
      if (!simulateWAH && current == exception) {
        current--;
      }
      return current;
    }

    @Override
    public void skipAllAfter(int i)
    {
      if (i >= current) {
        return;
      }
      current = i + 1;
    }

    @Override
    public void skipAllBefore(int i)
    {
      if (i <= current) {
        return;
      }
      current = i - 1;
    }

    @Override
    public void reset(int offset, int word, boolean fromBeginning)
    {
      if (!isOneSequence(word)) {
        throw new RuntimeException("NOT a sequence of ones!");
      }
      firstInt = offset;
      lastInt = offset + maxLiteralLengthMultiplication(getSequenceCount(word) + 1) - 1;
      if (!simulateWAH) {
        exception = offset + ((0x3FFFFFFF & word) >>> 25) - 1;
        if (exception == firstInt) {
          firstInt++;
        }
        if (exception == lastInt) {
          lastInt--;
        }
      }
      current = fromBeginning ? (firstInt - 1) : (lastInt + 1);
    }
  }

  /**
   * Iterator for all the integers of a  {@link ImmutableConciseSet}  instance
   */
  private class BitIterator implements IntIterator
  {
    /**
     * @uml.property name="litExp"
     * @uml.associationEnd
     */
    final LiteralAndZeroFillExpander litExp = new LiteralAndZeroFillExpander();
    /**
     * @uml.property name="oneExp"
     * @uml.associationEnd
     */
    final OneFillExpander oneExp = new OneFillExpander();
    /**
     * @uml.property name="exp"
     * @uml.associationEnd
     */
    WordExpander exp;
    int nextIndex = 0;
    int nextOffset = 0;

    private void nextWord()
    {
      final int word = words.get(nextIndex++);
      exp = isOneSequence(word) ? oneExp : litExp;
      exp.reset(nextOffset, word, true);

      // prepare next offset
      if (isLiteral(word)) {
        nextOffset += MAX_LITERAL_LENGHT;
      } else {
        nextOffset += maxLiteralLengthMultiplication(getSequenceCount(word) + 1);
      }
    }

    private BitIterator()
    {
      nextWord();
    }

    @Override
    public boolean hasNext()
    {
      return nextIndex <= lastWordIndex || exp.hasNext();
    }

    @Override
    public int next()
    {
      while (!exp.hasNext()) {
        if (nextIndex > lastWordIndex) {
          throw new NoSuchElementException();
        }
        nextWord();
      }
      return exp.next();
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void skipAllBefore(int element)
    {
      while (true) {
        exp.skipAllBefore(element);
        if (exp.hasNext() || nextIndex > lastWordIndex) {
          return;
        }
        nextWord();
      }
    }
  }

  /**
   * @author alessandrocolantonio
   */
  private class ReverseBitIterator implements IntIterator
  {
    /**
     * @uml.property name="litExp"
     * @uml.associationEnd
     */
    final LiteralAndZeroFillExpander litExp = new LiteralAndZeroFillExpander();
    /**
     * @uml.property name="oneExp"
     * @uml.associationEnd
     */
    final OneFillExpander oneExp = new OneFillExpander();
    /**
     * @uml.property name="exp"
     * @uml.associationEnd
     */
    WordExpander exp;
    int nextIndex = lastWordIndex;
    int nextOffset = maxLiteralLengthMultiplication(maxLiteralLengthDivision(last) + 1);
    int firstIndex; // first non-zero block

    void previousWord()
    {
      final int word = words.get(nextIndex--);
      exp = isOneSequence(word) ? oneExp : litExp;
      if (isLiteral(word)) {
        nextOffset -= MAX_LITERAL_LENGHT;
      } else {
        nextOffset -= maxLiteralLengthMultiplication(getSequenceCount(word) + 1);
      }
      exp.reset(nextOffset, word, false);
    }

    ReverseBitIterator()
    {
      // identify the first non-zero block
      if ((isSequenceWithNoBits(words.get(0)) && isZeroSequence(words.get(0))) || (isLiteral(words.get(0))
                                                                                   && words.get(0)
                                                                                      == ALL_ZEROS_LITERAL)) {
        firstIndex = 1;
      } else {
        firstIndex = 0;
      }
      previousWord();
    }

    @Override
    public boolean hasNext()
    {
      return nextIndex >= firstIndex || exp.hasPrevious();
    }

    @Override
    public int next()
    {
      while (!exp.hasPrevious()) {
        if (nextIndex < firstIndex) {
          throw new NoSuchElementException();
        }
        previousWord();
      }
      return exp.previous();
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void skipAllBefore(int element)
    {
      while (true) {
        exp.skipAllAfter(element);
        if (exp.hasPrevious() || nextIndex < firstIndex) {
          return;
        }
        previousWord();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntIterator iterator()
  {
    if (isEmpty()) {
      return new IntIterator()
      {
        @Override
        public void skipAllBefore(int element) {/*empty*/}

        @Override
        public boolean hasNext() {return false;}

        @Override
        public int next() {throw new NoSuchElementException();}

        @Override
        public void remove() {throw new UnsupportedOperationException();}
      };
    }
    return new BitIterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntIterator descendingIterator()
  {
    if (isEmpty()) {
      return new IntIterator()
      {
        @Override
        public void skipAllBefore(int element) {/*empty*/}

        @Override
        public boolean hasNext() {return false;}

        @Override
        public int next() {throw new NoSuchElementException();}

        @Override
        public void remove() {throw new UnsupportedOperationException();}
      };
    }
    return new ReverseBitIterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int last()
  {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return last;
  }

  /**
   * Convert a given collection to a {@link ImmutableConciseSet} instance
   */
  private ImmutableConciseSet convert(IntSet c)
  {
    if (c instanceof ImmutableConciseSet && simulateWAH == ((ImmutableConciseSet) c).simulateWAH) {
      return (ImmutableConciseSet) c;
    }
    if (c == null) {
      return new ImmutableConciseSet((IntBuffer) null);
    }

    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ImmutableConciseSet convert(int... a)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ImmutableConciseSet convert(Collection<Integer> c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean add(int e)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean remove(int o)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(int o)
  {
    if (isEmpty() || o > last || o < 0) {
      return false;
    }

    // check if the element is within a literal word
    int block = maxLiteralLengthDivision(o);
    int bit = maxLiteralLengthModulus(o);
    for (int i = 0; i <= lastWordIndex; i++) {
      final int w = words.get(i);
      final int t = w & 0xC0000000; // the first two bits...
      switch (t) {
        case 0x80000000:  // LITERAL
        case 0xC0000000:  // LITERAL
          // check if the current literal word is the "right" one
          if (block == 0) {
            return (w & (1 << bit)) != 0;
          }
          block--;
          break;
        case 0x00000000:  // ZERO SEQUENCE
          if (!simulateWAH) {
            if (block == 0 && ((w >> 25) - 1) == bit) {
              return true;
            }
          }
          block -= getSequenceCount(w) + 1;
          if (block < 0) {
            return false;
          }
          break;
        case 0x40000000:  // ONE SEQUENCE
          if (!simulateWAH) {
            if (block == 0 && (0x0000001F & (w >> 25) - 1) == bit) {
              return false;
            }
          }
          block -= getSequenceCount(w) + 1;
          if (block < 0) {
            return true;
          }
          break;
      }
    }

    // no more words
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsAll(IntSet c)
  {
    if (c == null || c.isEmpty() || c == this) {
      return true;
    }
    if (isEmpty()) {
      return false;
    }

    final ImmutableConciseSet other = convert(c);
    if (other.last > last) {
      return false;
    }
    if (size >= 0 && other.size > size) {
      return false;
    }
    if (other.size == 1) {
      return contains(other.last);
    }

    // check whether the first operator starts with a sequence that
    // completely "covers" the second operator
    if (isSequenceWithNoBits(this.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(this.words.get(0)) + 1) > other.last) {
      if (isZeroSequence(this.words.get(0))) {
        return false;
      }
      return true;
    }
    if (isSequenceWithNoBits(other.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(other.words.get(0)) + 1) > this.last) {
      return false;
    }

    // scan "this" and "other"
    WordIterator thisItr = new WordIterator();
    WordIterator otherItr = other.new WordIterator();
    while (true) {
      if (!thisItr.isLiteral) {
        if (!otherItr.isLiteral) {
          int minCount = Math.min(thisItr.count, otherItr.count);
          if ((SEQUENCE_BIT & thisItr.word) == 0 && (SEQUENCE_BIT & otherItr.word) != 0) {
            return false;
          }
          if (!otherItr.prepareNext(minCount)) {
            return true;
          }
          if (!thisItr.prepareNext(minCount)) {
            return false;
          }
        } else {
          if ((thisItr.toLiteral() & otherItr.word) != otherItr.word) {
            return false;
          }
          thisItr.word--;
          if (!otherItr.prepareNext()) {
            return true;
          }
          if (!thisItr.prepareNext(1)) {
            return false;
          }
        }
      } else if (!otherItr.isLiteral) {
        int o = otherItr.toLiteral();
        if ((thisItr.word & otherItr.toLiteral()) != o) {
          return false;
        }
        otherItr.word--;
        if (!otherItr.prepareNext(1)) {
          return true;
        }
        if (!thisItr.prepareNext()) {
          return false;
        }
      } else {
        if ((thisItr.word & otherItr.word) != otherItr.word) {
          return false;
        }
        if (!otherItr.prepareNext()) {
          return true;
        }
        if (!thisItr.prepareNext()) {
          return false;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsAny(IntSet c)
  {
    if (c == null || c.isEmpty() || c == this) {
      return true;
    }
    if (isEmpty()) {
      return false;
    }

    final ImmutableConciseSet other = convert(c);
    if (other.size == 1) {
      return contains(other.last);
    }

    // disjoint sets
    if (isSequenceWithNoBits(this.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(this.words.get(0)) + 1) > other.last) {
      if (isZeroSequence(this.words.get(0))) {
        return false;
      }
      return true;
    }
    if (isSequenceWithNoBits(other.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(other.words.get(0)) + 1) > this.last) {
      if (isZeroSequence(other.words.get(0))) {
        return false;
      }
      return true;
    }

    // scan "this" and "other"
    WordIterator thisItr = new WordIterator();
    WordIterator otherItr = other.new WordIterator();
    while (true) {
      if (!thisItr.isLiteral) {
        if (!otherItr.isLiteral) {
          int minCount = Math.min(thisItr.count, otherItr.count);
          if ((SEQUENCE_BIT & thisItr.word & otherItr.word) != 0) {
            return true;
          }
          if (!thisItr.prepareNext(minCount) | !otherItr.prepareNext(minCount)) // NOT ||
          {
            return false;
          }
        } else {
          if ((thisItr.toLiteral() & otherItr.word) != ALL_ZEROS_LITERAL) {
            return true;
          }
          thisItr.word--;
          if (!thisItr.prepareNext(1) | !otherItr.prepareNext()) // do NOT use "||"
          {
            return false;
          }
        }
      } else if (!otherItr.isLiteral) {
        if ((thisItr.word & otherItr.toLiteral()) != ALL_ZEROS_LITERAL) {
          return true;
        }
        otherItr.word--;
        if (!thisItr.prepareNext() | !otherItr.prepareNext(1)) // do NOT use  "||"
        {
          return false;
        }
      } else {
        if ((thisItr.word & otherItr.word) != ALL_ZEROS_LITERAL) {
          return true;
        }
        if (!thisItr.prepareNext() | !otherItr.prepareNext()) // do NOT use  "||"
        {
          return false;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsAtLeast(IntSet c, int minElements)
  {
    if (minElements < 1) {
      throw new IllegalArgumentException();
    }
    if ((size >= 0 && size < minElements) || c == null || c.isEmpty() || isEmpty()) {
      return false;
    }
    if (this == c) {
      return size() >= minElements;
    }

    // convert the other set in order to perform a more complex intersection
    ImmutableConciseSet other = convert(c);
    if (other.size >= 0 && other.size < minElements) {
      return false;
    }
    if (minElements == 1 && other.size == 1) {
      return contains(other.last);
    }
    if (minElements == 1 && size == 1) {
      return other.contains(last);
    }

    // disjoint sets
    if (isSequenceWithNoBits(this.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(this.words.get(0)) + 1) > other.last) {
      if (isZeroSequence(this.words.get(0))) {
        return false;
      }
      return true;
    }
    if (isSequenceWithNoBits(other.words.get(0))
        && maxLiteralLengthMultiplication(getSequenceCount(other.words.get(0)) + 1) > this.last) {
      if (isZeroSequence(other.words.get(0))) {
        return false;
      }
      return true;
    }

    // resulting size
    int res = 0;

    // scan "this" and "other"
    WordIterator thisItr = new WordIterator();
    WordIterator otherItr = other.new WordIterator();
    while (true) {
      if (!thisItr.isLiteral) {
        if (!otherItr.isLiteral) {
          int minCount = Math.min(thisItr.count, otherItr.count);
          if ((SEQUENCE_BIT & thisItr.word & otherItr.word) != 0) {
            res += maxLiteralLengthMultiplication(minCount);
            if (res >= minElements) {
              return true;
            }
          }
          if (!thisItr.prepareNext(minCount) | !otherItr.prepareNext(minCount)) // NOT ||
          {
            return false;
          }
        } else {
          res += getLiteralBitCount(thisItr.toLiteral() & otherItr.word);
          if (res >= minElements) {
            return true;
          }
          thisItr.word--;
          if (!thisItr.prepareNext(1) | !otherItr.prepareNext()) // do NOT use "||"
          {
            return false;
          }
        }
      } else if (!otherItr.isLiteral) {
        res += getLiteralBitCount(thisItr.word & otherItr.toLiteral());
        if (res >= minElements) {
          return true;
        }
        otherItr.word--;
        if (!thisItr.prepareNext() | !otherItr.prepareNext(1)) // do NOT use  "||"
        {
          return false;
        }
      } else {
        res += getLiteralBitCount(thisItr.word & otherItr.word);
        if (res >= minElements) {
          return true;
        }
        if (!thisItr.prepareNext() | !otherItr.prepareNext()) // do NOT use  "||"
        {
          return false;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty()
  {
    return words == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean retainAll(IntSet c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addAll(IntSet c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeAll(IntSet c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size()
  {
    if (size < 0) {
      size = 0;
      for (int i = 0; i <= lastWordIndex; i++) {
        int w = words.get(i);
        if (isLiteral(w)) {
          size += getLiteralBitCount(w);
        } else {
          if (isZeroSequence(w)) {
            if (!isSequenceWithNoBits(w)) {
              size++;
            }
          } else {
            size += maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
            if (!isSequenceWithNoBits(w)) {
              size--;
            }
          }
        }
      }
    }
    return size;
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableConciseSet empty() {
		return new ImmutableConciseSet();
	}

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + (words != null ? words.hashCode() : 0);
    result = 31 * result + last;
    result = 31 * result + size;
    result = 31 * result + lastWordIndex;
    result = 31 * result + (simulateWAH ? 1 : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    ImmutableConciseSet that = (ImmutableConciseSet) o;

    if (last != that.last) {
      return false;
    }
    if (lastWordIndex != that.lastWordIndex) {
      return false;
    }

    if (simulateWAH != that.simulateWAH) {
      return false;
    }
    if (size != that.size) {
      return false;
    }
    if (words != null ? !words.equals(that.words) : that.words != null) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(IntSet o)
  {
    // empty set cases
    if (this.isEmpty() && o.isEmpty()) {
      return 0;
    }
    if (this.isEmpty()) {
      return -1;
    }
    if (o.isEmpty()) {
      return 1;
    }

    final ImmutableConciseSet other = convert(o);

    // the word at the end must be the same
    int res = this.last - other.last;
    if (res != 0) {
      return res < 0 ? -1 : 1;
    }

    // scan words from MSB to LSB
    int thisIndex = this.lastWordIndex;
    int otherIndex = other.lastWordIndex;
    int thisWord = this.words.get(thisIndex);
    int otherWord = other.words.get(otherIndex);
    while (thisIndex >= 0 && otherIndex >= 0) {
      if (!isLiteral(thisWord)) {
        if (!isLiteral(otherWord)) {
          // compare two sequences
          // note that they are made up of at least two blocks, and we
          // start comparing from the end, that is at blocks with no
          // (un)set bits
          if (isZeroSequence(thisWord)) {
            if (isOneSequence(otherWord))
            // zeros < ones
            {
              return -1;
            }
            // compare two sequences of zeros
            res = getSequenceCount(otherWord) - getSequenceCount(thisWord);
            if (res != 0) {
              return res < 0 ? -1 : 1;
            }
          } else {
            if (isZeroSequence(otherWord))
            // ones > zeros
            {
              return 1;
            }
            // compare two sequences of ones
            res = getSequenceCount(thisWord) - getSequenceCount(otherWord);
            if (res != 0) {
              return res < 0 ? -1 : 1;
            }
          }
          // if the sequences are the same (both zeros or both ones)
          // and have the same length, compare the first blocks in the
          // next loop since such blocks might contain (un)set bits
          thisWord = getLiteral(thisWord);
          otherWord = getLiteral(otherWord);
        } else {
          // zeros < literal --> -1
          // ones > literal --> +1
          // note that the sequence is made up of at least two blocks,
          // and we start comparing from the end, that is at a block
          // with no (un)set bits
          if (isZeroSequence(thisWord)) {
            if (otherWord != ALL_ZEROS_LITERAL) {
              return -1;
            }
          } else {
            if (otherWord != ALL_ONES_LITERAL) {
              return 1;
            }
          }
          if (getSequenceCount(thisWord) == 1) {
            thisWord = getLiteral(thisWord);
          } else {
            thisWord--;
          }
          if (--otherIndex >= 0) {
            otherWord = other.words.get(otherIndex);
          }
        }
      } else if (!isLiteral(otherWord)) {
        // literal > zeros --> +1
        // literal < ones --> -1
        // note that the sequence is made up of at least two blocks,
        // and we start comparing from the end, that is at a block
        // with no (un)set bits
        if (isZeroSequence(otherWord)) {
          if (thisWord != ALL_ZEROS_LITERAL) {
            return 1;
          }
        } else {
          if (thisWord != ALL_ONES_LITERAL) {
            return -1;
          }
        }
        if (--thisIndex >= 0) {
          thisWord = this.words.get(thisIndex);
        }
        if (getSequenceCount(otherWord) == 1) {
          otherWord = getLiteral(otherWord);
        } else {
          otherWord--;
        }
      } else {
        res = thisWord - otherWord; // equals getLiteralBits(thisWord) - getLiteralBits(otherWord)
        if (res != 0) {
          return res < 0 ? -1 : 1;
        }
        if (--thisIndex >= 0) {
          thisWord = this.words.get(thisIndex);
        }
        if (--otherIndex >= 0) {
          otherWord = other.words.get(otherIndex);
        }
      }
    }
    return thisIndex >= 0 ? 1 : (otherIndex >= 0 ? -1 : 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear(int from, int to)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fill(int from, int to)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flip(int e)
  {
    throw new UnsupportedOperationException();
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (lastWordIndex + 1) / Math.ceil((1 + last) / 32D);
	}

  /**
   * {@inheritDoc}
   */
  @Override
  public double collectionCompressionRatio() {
    if (isEmpty())
      return 0D;
    return (double) (lastWordIndex + 1) / size();
  }

  /*
   * DEBUGGING METHODS
   */

	/**
	 * Generates the 32-bit binary representation of a given word (debug only)
	 *
	 * @param word
	 *            word to represent
	 * @return 32-character string that represents the given word
	 */
	private static String toBinaryString(int word) {
		String lsb = Integer.toBinaryString(word);
		StringBuilder pad = new StringBuilder();
		for (int i = lsb.length(); i < 32; i++)
			pad.append('0');
		return pad.append(lsb).toString();
	}

  /**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		final StringBuilder s = new StringBuilder("INTERNAL REPRESENTATION:\n");
		final Formatter f = new Formatter(s, Locale.ENGLISH);

		if (isEmpty())
			return s.append("null\n").toString();

		f.format("Elements: %s\n", toString());

		// elements
		int firstBitInWord = 0;
		for (int i = 0; i <= lastWordIndex; i++) {
			// raw representation of words[i]
			f.format("words[%d] = ", i);
			String ws = toBinaryString(words.get(i));
			if (isLiteral(words.get(i))) {
				s.append(ws.substring(0, 1));
				s.append("--");
				s.append(ws.substring(1));
			} else {
				s.append(ws.substring(0, 2));
				s.append('-');
				if (simulateWAH)
					s.append("xxxxx");
				else
					s.append(ws.substring(2, 7));
				s.append('-');
				s.append(ws.substring(7));
			}
			s.append(" --> ");

			// decode words[i]
			if (isLiteral(words.get(i))) {
				// literal
				s.append("literal: ");
				s.append(toBinaryString(words.get(i)).substring(1));
				f.format(" ---> [from %d to %d] ", firstBitInWord, firstBitInWord + MAX_LITERAL_LENGHT - 1);
				firstBitInWord += MAX_LITERAL_LENGHT;
			} else {
				// sequence
				if (isOneSequence(words.get(i))) {
					s.append('1');
				} else {
					s.append('0');
				}
				s.append(" block: ");
				s.append(toBinaryString(getLiteralBits(getLiteral(words.get(i)))).substring(1));
				if (!simulateWAH) {
					s.append(" (bit=");
					int bit = (words.get(i) & 0x3E000000) >>> 25;
					if (bit == 0)
						s.append("none");
					else
						s.append(String.format("%4d", bit - 1));
					s.append(')');
				}
				int count = getSequenceCount(words.get(i));
				f.format(" followed by %d blocks (%d bits)",
						getSequenceCount(words.get(i)),
						maxLiteralLengthMultiplication(count));
				f.format(" ---> [from %d to %d] ", firstBitInWord, firstBitInWord + (count + 1) * MAX_LITERAL_LENGHT - 1);
				firstBitInWord += (count + 1) * MAX_LITERAL_LENGHT;
			}
			s.append('\n');
		}

		// object attributes
		f.format("simulateWAH: %b\n", simulateWAH);
		f.format("last: %d\n", last);
		f.format("size: %s\n", (size == -1 ? "invalid" : Integer.toString(size)));
		f.format("words.length: %d\n", words.capacity());
		f.format("lastWordIndex: %d\n", lastWordIndex);

		// compression
		f.format("bitmap compression: %.2f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.2f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}
}
