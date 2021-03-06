package alix.util;

import java.util.Arrays;

/**
 * A fixed list of ints. Has a hashCode implementation, such object is a good
 * key for HashMaps.
 * 
 * @author glorieux-f
 *
 */
public class IntTuple implements Comparable<IntTuple>
{
    /** Internal data */
    protected int[] data; // could be final for a tuple
    /** Size of tuple */
    protected int size; // could be final for a tuple
    /** HashCode cache */
    protected int hash; // could be final for a tuple

    /**
     * Empty constructor, no sense for a non mutable Tuple, but useful for mutable.
     */
    public IntTuple() {
        data = new int[4];
    }

    /**
     * Build a pair
     * 
     * @param a
     * @param b
     */
    public IntTuple(int a, int b) {
        size = 2;
        data = new int[size];
        data[0] = a;
        data[1] = b;
    }

    /**
     * Build a 3-tuple
     * 
     * @param a
     * @param b
     * @param c
     */
    public IntTuple(int a, int b, int c) {
        size = 3;
        data = new int[size];
        data[0] = a;
        data[1] = b;
        data[2] = c;
    }

    /**
     * Build a 4-tuple
     * 
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public IntTuple(int a, int b, int c, int d) {
        size = 4;
        data = new int[size];
        data[0] = a;
        data[1] = b;
        data[2] = c;
        data[3] = d;
    }

    /**
     * Take a copy of an int array.
     * 
     * @param data
     */
    public IntTuple(int[] data) {
        this.data = new int[data.length];
        put(data);
    }

    /**
     * Take a copy of an int array.
     * 
     * @param data
     */
    protected void put(int[] data)
    {
        this.size = data.length;
        onWrite(this.size - 1);
        System.arraycopy(data, 0, this.data, 0, size);
    }

    /**
     * Build a tuple from another tuple.
     * 
     * @param tuple
     */
    public IntTuple(IntTuple tuple) {
        data = new int[tuple.size];
        put(tuple);
    }

    /**
     * Take a copy of another set.
     * 
     * @param tuple
     */
    protected void put(IntTuple tuple)
    {
        int length = tuple.size;
        this.size = length;
        onWrite(length - 1);
        System.arraycopy(tuple.data, 0, data, 0, length);
    }

    /**
     * Take a copy of an int roller
     * 
     * @param roller
     */
    public IntTuple(IntRoller roller) {
        data = new int[roller.size()];
        put(roller);
    }

    /**
     * Take a copy of an int roller
     * 
     * @param roller
     */
    protected void put(IntRoller roller)
    {
        this.size = roller.size();
        onWrite(this.size - 1);
        int lim = roller.right;
        int j = 0;
        for (int i = roller.left; i < lim; i++) {
            data[j] = roller.get(i);
            j++;
        }
    }

    /**
     * Call it before write
     * 
     * @param position
     * @return true if resized (? good ?)
     */
    protected boolean onWrite(final int position)
    {
        hash = 0;
        if (position < data.length)
            return false;
        final int oldLength = data.length;
        final int[] oldData = data;
        int capacity = Calcul.nextSquare(position + 1);
        data = new int[capacity];
        System.arraycopy(oldData, 0, data, 0, oldLength);
        if (position >= size)
            size = (position + 1);
        return true;
    }

    /**
     * Get int at a position.
     * 
     * @param pos
     * @return
     */
    public int get(int pos)
    {
        return data[pos];
    }

    /**
     * Size of data.
     * 
     * @return
     */
    public int size()
    {
        return size;
    }

    /**
     * 
     * @return
     */
    public int[] toArray()
    {
        return toArray(null);
    }

    /**
     * Fill the provided array with sorted values, or create new if null provided
     * 
     * @param dest
     * @return
     */
    public int[] toArray(int[] dest)
    {
        if (dest == null)
            dest = new int[size];
        int lim = Math.min(dest.length, size);
        System.arraycopy(data, 0, dest, 0, lim);
        // if provided array is bigger than size, do not sort with other values
        Arrays.sort(dest, 0, lim);
        return dest;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (o instanceof IntTuple) {
            IntTuple phr = (IntTuple) o;
            if (phr.size != size)
                return false;
            for (short i = 0; i < size; i++) {
                if (phr.data[i] != data[i])
                    return false;
            }
            return true;
        }
        if (o instanceof IntRoller) {
            IntRoller roll = (IntRoller) o;
            if (roll.size() != size)
                return false;
            int i = size - 1;
            int iroll = roll.right;
            do {
                if (roll.get(iroll) != data[i])
                    return false;
                i--;
                iroll--;
            }
            while (i >= 0);
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(IntTuple tuple)
    {
        if (size != tuple.size)
            return Integer.compare(size, tuple.size);
        int lim = size; // avoid a field lookup
        for (int i = 0; i < lim; i++) {
            if (data[i] != tuple.data[i])
                return Integer.compare(data[i], tuple.data[i]);
        }
        return 0;
    }

    @Override
    public int hashCode()
    {
        if (hash != 0)
            return hash;
        int res = 17;
        for (int i = 0; i < size; i++) {
            res = 31 * res + data[i];
        }
        return res;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(data[i]);
        }
        sb.append(')');
        return sb.toString();
    }
    

    public static void main(String[] args) throws Exception
    {
        long start = System.nanoTime();
        // test hashcode function
        IntPair pair = new IntPair();
        IntVek hashtest = new IntVek();
        SparseMat.Counter counter;
        double tot = 0;
        double col = 0;
        for (int i = 0; i < 50000; i+=3) {
            pair.x(i);
            for (int j = 0; j < 50000; j+=7) {
                tot++;
                pair.y(j);
                int hashcode = pair.hashCode();
                int ret = hashtest.inc(hashcode);
                if (ret > 1) col++;
            }
        }
        System.out.println("collisions="+col+" total="+tot+" collisions/total="+col/tot);
        System.out.println(((System.nanoTime() - start) / 1000000) + " ms");
        System.exit(2);
        IntPair[] a = {new IntPair(2, 1), new IntPair(1, 2), new IntPair(1, 1), new IntPair(2, 2)};
        Arrays.sort(a);
        for(IntPair p: a) System.out.println(p);
    }


}
