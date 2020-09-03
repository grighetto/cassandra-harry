/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package harry.generators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

// TODO: document
public class StringBijection implements Bijections.Bijection<String>
{
    public static final int NIBBLES_SIZE = 256;
    private final String[] nibbles;
    private final Map<String, Integer> inverse;
    private final int nibbleSize;
    private final int maxRandomNibbles;

    public StringBijection()
    {
        this(alphabetNibbles(8), 8, 10);
    }

    public StringBijection(int nibbleSize, int maxRandomNibbles)
    {
        this(alphabetNibbles(nibbleSize), nibbleSize, maxRandomNibbles);
    }

    public StringBijection(String[] nibbles, int nibbleSize, int maxRandomNibbles)
    {
        assert nibbles.length == NIBBLES_SIZE;
        this.nibbles = nibbles;
        this.inverse = new HashMap<>();
        this.nibbleSize = nibbleSize;

        for (int i = 0; i < nibbles.length; i++)
        {
            assert nibbles[i].length() == nibbleSize;
            inverse.put(nibbles[i], i);
        }

        this.maxRandomNibbles = maxRandomNibbles;
    }

    public String inflate(long descriptor)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Long.BYTES; i++)
        {
            int idx = getByte(descriptor, i);
            builder.append(nibbles[idx]);
        }

        appendRandomNibbles(builder, descriptor);

        // everything after this point can be just random, since strings are guaranteed
        // to have unique prefixes
        return builder.toString();
    }

    public static int getByte(long l, int idx)
    {
        int b = (int) ((l >> (Long.BYTES - idx - 1) * Byte.SIZE) & 0xff);
        if (idx == 0)
            b ^= 0x80;
        return b;
    }


    // TODO: shuld we switch to PCG here, too?
    private void appendRandomNibbles(StringBuilder builder, long descriptor)
    {
        long rnd = RngUtils.next(descriptor);
        int count = RngUtils.asInt(rnd, 0, maxRandomNibbles);

        for (int i = 0; i < count; i++)
        {
            rnd = RngUtils.next(rnd);
            builder.append(nibbles[RngUtils.asByte(rnd) & 0xff]);
        }
    }

    public long deflate(String descriptor)
    {
        long res = 0;
        for (int i = 0; i < Long.BYTES; i++)
        {
            String nibble = descriptor.substring(nibbleSize * i, nibbleSize * (i + 1));
            assert inverse.containsKey(nibble) : String.format("Bad string: %s, %s", nibble, descriptor);
            long idx = inverse.get(nibble);
            if (i == 0)
                idx ^= 0x80;
            res |= idx << (Long.BYTES - i - 1) * Byte.SIZE;
        }
        return res;
    }

    public int compare(long l, long r)
    {
        for (int i = 0; i < Long.BYTES; i++)
        {
            int cmp = Integer.compare(getByte(l, i),
                                      getByte(r, i));
            if (cmp != 0)
                return cmp;
        }
        return 0;
    }

    public int byteSize()
    {
        return Long.BYTES;
    }

    @Override
    public String toString()
    {
        return "ascii(" +
               "nibbleSize=" + nibbleSize +
               ", maxRandomNibbles=" + maxRandomNibbles +
               ')';
    }

    // TODO: create a method that would generate random nibbles, and return them sorted
    private final static char[] characters = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                                               'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

    // We don't really care much about algorithmic complexity of this method since it's called only once during startup
    public static String[] alphabetNibbles(int nibbleSize)
    {
        Random rnd = new Random(1);

        // We need to generate 256 _unique_ strings; ideally we don't want to re-sort them
        Set<String> strings = new TreeSet<String>();
        while (strings.size() < NIBBLES_SIZE)
        {
            char[] chars = new char[nibbleSize];
            for (int j = 0; j < nibbleSize; j++)
                chars[j] = characters[rnd.nextInt(characters.length)];

            strings.add(new String(chars));
        }

        assert strings.size() == NIBBLES_SIZE : strings.size();
        String[] nibbles = new String[NIBBLES_SIZE];
        Iterator<String> iter = strings.iterator();
        for (int i = 0; i < NIBBLES_SIZE; i++)
            nibbles[i] = iter.next();

        return nibbles;
    }
}