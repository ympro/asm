/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.objectweb.asm;

import java.util.Arrays;

/**
 * A class that represent a constant dynamic.
 * 
 * @author Remi Forax
 */
public final class Condy {
    final String name;
    final String desc;
    final Handle bsm;
    final Object[] bsmArgs;
    
    /**
     * Create a constant dynamic.
     * @param name an arbitrary name 
     * @param desc a field descriptor
     * @param bsm a bootstrap method
     * @param bsmArgs the arguments of the bootstrap method
     */
    public Condy(String name, String desc, Handle bsm, Object... bsmArgs) {
        this.name = name;
        this.desc = desc;
        this.bsm = bsm;
        this.bsmArgs = bsmArgs;
    }
    
    /**
     * Returns the name of the current constant dynamic.
     * @return the name of the current constant dynamic.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the descriptor (a field descriptor) of the current constant dynamic.
     * @return the descriptor (a field descriptor) of the current constant dynamic.
     */
    public String getDesc() {
        return desc;
    }
  
    /**
     * Returns the boostrap method of the current constant dynamic.
     * @return the boostrap method of the current constant dynamic.
     */
    public Handle getBsm() {
        return bsm;
    }
    
    /**
     * Returns the bootstrap method arguments of the current constant dynamic.
     * @return the bootstrap method arguments of the current constant dynamic.
     */
    public Object[] getBsmArgs() {
        return bsmArgs;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Condy)) {
            return false;
        }
        Condy condy = (Condy)obj;
        return name.equals(condy.name) &&
               desc.equals(condy.desc) &&
               bsm.equals(condy.bsm) &&
               Arrays.equals(bsmArgs, condy.bsmArgs);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() ^
               Integer.rotateLeft(desc.hashCode(), 8) ^
               Integer.rotateLeft(bsm.hashCode(), 16) ^
               Integer.rotateLeft(Arrays.hashCode(bsmArgs), 24);
    }
    
    @Override
    public String toString() {
        return name + " : " + desc + ' ' + bsm + ' ' + Arrays.toString(bsmArgs);
    }
}
