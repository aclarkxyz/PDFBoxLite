/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fontbox.cmap;


/**
 * This represents a single entry in the codespace range.
 *
 * @author Ben Litchfield
 */
public class CodespaceRange
{
    private byte[] start;
    private byte[] end;
    private int codeLength = 0;
    
    /**
     * Creates a new instance of CodespaceRange.
     */
    public CodespaceRange()
    {
    }

    /**
     * Returns the length of the codes of the codespace.
     * 
     * @return the code length
     */
    public int getCodeLength()
    {
        return codeLength;
    }

    /** Getter for property end.
     * @return Value of property end.
     *
     */
    public byte[] getEnd()
    {
        return end;
    }

    /** Setter for property end.
     * @param endBytes New value of property end.
     *
     */
    void setEnd(byte[] endBytes)
    {
        end = endBytes;
    }

    /** Getter for property start.
     * @return Value of property start.
     *
     */
    public byte[] getStart()
    {
        return start;
    }

    /** Setter for property start.
     * @param startBytes New value of property start.
     *
     */
    void setStart(byte[] startBytes)
    {
        start = startBytes;
        codeLength = start.length;
    }

    /**
     * Returns true if the given code bytes match this codespace range.
     */
    public boolean matches(byte[] code)
    {
        return isFullMatch(code, code.length);
    }

    /**
     * Returns true if the given code bytes match this codespace range.
     */
    public boolean isFullMatch(byte[] code, int codeLen)
    {
        // code must be the same length as the bounding codes
        if (codeLen == codeLength)
        {
            // each of it bytes must lie between the corresponding bytes of the upper & lower bounds
            for (int i = 0; i < codeLen; i++)
            {
                int startNum = start[i] & 0xff;
                int endNum = end[i] & 0xff;
                int codeNum = code[i] & 0xff;

                if (codeNum > endNum || codeNum < startNum)
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the given byte matches the byte at the given index of this codespace range.
     */
    public boolean isPartialMatch(byte b, int index)
    {
        if (index == codeLength)
        {
            return false;
        }
        int startNum = start[index] & 0xff;
        int endNum = end[index] & 0xff;
        int codeNum = b & 0xff;
        return !(codeNum > endNum || codeNum < startNum);
    }
}