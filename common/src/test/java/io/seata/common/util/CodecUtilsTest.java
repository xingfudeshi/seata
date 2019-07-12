/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Geng Zhang
 */
class CodecUtilsTest {

    @Test
    public void parseHigh4Low4Bytes() throws Exception {
        byte b = 117; // = 7*16+5
        byte[] bs = CodecUtils.parseHigh4Low4Bytes(b);
        Assertions.assertEquals(bs[0], 7);
        Assertions.assertEquals(bs[1], 5);
    }

    @Test
    public void buildHigh4Low4Bytes() throws Exception {
        byte bs = CodecUtils.buildHigh4Low4Bytes((byte) 7, (byte) 5);
        Assertions.assertEquals(bs, (byte) 117);
    }
}