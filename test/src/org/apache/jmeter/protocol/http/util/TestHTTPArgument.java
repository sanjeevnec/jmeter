/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.http.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestHTTPArgument {

        @Rule
        public final ExpectedException exception = ExpectedException.none();

        @Test
        public void testCloning() throws Exception {
            HTTPArgument arg = new HTTPArgument("name.?", "value_ here");
            assertEquals("name.?", arg.getName());
            assertEquals("value_ here", arg.getValue());
            assertEquals("name.%3F", arg.getEncodedName());
            assertEquals("value_+here", arg.getEncodedValue());
            HTTPArgument clone = (HTTPArgument) arg.clone();
            assertEquals("name.%3F", clone.getEncodedName());
            assertEquals("value_+here", clone.getEncodedValue());
            assertEquals("name.?", clone.getName());
            assertEquals("value_ here", clone.getValue());
        }

        @Test
        public void testConversion() throws Exception {
            Arguments args = new Arguments();
            args.addArgument("name.?", "value_ here");
            args.addArgument("name$of property", "value_.+");
            HTTPArgument.convertArgumentsToHTTP(args);
            CollectionProperty argList = args.getArguments();
            HTTPArgument httpArg = (HTTPArgument) argList.get(0).getObjectValue();
            assertEquals("name.%3F", httpArg.getEncodedName());
            assertEquals("value_+here", httpArg.getEncodedValue());
            httpArg = (HTTPArgument) argList.get(1).getObjectValue();
            assertEquals("name%24of+property", httpArg.getEncodedName());
            assertEquals("value_.%2B", httpArg.getEncodedValue());
        }

        @Test
        public void testEncoding() throws Exception {
            HTTPArgument arg;
            arg = new HTTPArgument("name.?", "value_ here", false);
            assertEquals("name.?", arg.getName());
            assertEquals("value_ here", arg.getValue());
            assertEquals("name.%3F", arg.getEncodedName());
            assertEquals("value_+here", arg.getEncodedValue());
            // Show that can bypass encoding:
            arg.setAlwaysEncoded(false);
            assertEquals("name.?", arg.getEncodedName());
            assertEquals("value_ here", arg.getEncodedValue());

            // The sample does not use a valid encoding
            arg = new HTTPArgument("name.?", "value_ here", true);
            assertEquals("name.?", arg.getName());
            assertEquals("value_ here", arg.getValue());
            assertEquals("name.%3F", arg.getEncodedName());
            assertEquals("value_+here", arg.getEncodedValue());
            arg.setAlwaysEncoded(false); // by default, name/value are encoded on fetch
            assertEquals("name.?", arg.getEncodedName());
            assertEquals("value_ here", arg.getEncodedValue());

            // Try a real encoded argument
            arg = new HTTPArgument("name.%3F", "value_+here", true);
            assertEquals("name.?", arg.getName());
            assertEquals("value_ here", arg.getValue());
            assertEquals("name.%3F", arg.getEncodedName());
            assertEquals("value_+here", arg.getEncodedValue());
            // Show that can bypass encoding:
            arg.setAlwaysEncoded(false);
            assertEquals("name.?", arg.getEncodedName());
            assertEquals("value_ here", arg.getEncodedValue());

            arg = new HTTPArgument("", "\00\01\07", "", false);
            arg.setAlwaysEncoded(false);
            assertEquals("", arg.getEncodedName());
            assertEquals("\00\01\07", arg.getEncodedValue());
        }

        @Test
        public void testShift_JISEncoding() throws Exception {
            testEncodings("Shift_JIS");
        }

        @Test
        public void testUS_ASCIIEncoding() throws Exception {
            testEncodings("US-ASCII");
        }

        @Test
        public void testCP1252Encoding() throws Exception {
            testEncodings("CP1252");
        }

        @Test
        public void testDecoderException() throws Exception {
            // When invalid string is passed IllegalArgumentException exception occurs
            exception.expect(IllegalArgumentException.class);
            exception.expectMessage("org.apache.commons.codec.DecoderException: Invalid URL encoding: not a valid digit");
            new HTTPArgument("s=*&^%~@==y", "\00\01\07", true, "UTF-8");
        }

        @Test
        public void testUnsupportedEncodingException() throws Exception {
            // When invalid encoding(UTF-9) is passed error occurs
            exception.expect(Error.class);
            exception.expectMessage("java.io.UnsupportedEncodingException: UTF-9");
            new HTTPArgument("name.?", "\00\01\07", true, "UTF-9");
        }

        @Test
        public void testWithShift_JIS_Encoding() throws Exception {
            String encodedValue = "%8F%89%8A%FA%92l%91%E5%8D%E3%8Es";
            byte[] data = new byte[] {-27, -120, -99, -26, -100, -97, -27, -128, -92, -27, -92, -89, -23, -104, -86, -27,
                    -72, -126};
            String value = new String(data, StandardCharsets.UTF_8);
            HTTPArgument arg;
            // Passing real encoded value of Japanese character as
            // %8F%89%8A%FA%92l%91%E5%8D%E3%8Es and try to decode with Shift_JIS encoding
            arg = new HTTPArgument("name.?", encodedValue, true, "Shift_JIS");
            assertEquals("HTTPArgument(\"name.?\", encodedValue, true, \"Shift_JIS\").getValue()", value, arg.getValue());
        }

        @Test
        public void testWithUTF_Encoding() throws Exception {
            String encodedValue = "%8F%89%8A%FA%92l%91%E5%8D%E3%8Es";
            byte[] data = new byte[] {-27, -120, -99, -26, -100, -97, -27, -128, -92, -27, -92, -89, -23, -104, -86, -27,
                    -72, -126};
            String value = new String(data, StandardCharsets.UTF_8);
            HTTPArgument arg;
            // Passing real encoded value of Japanese character as
            // %8F%89%8A%FA%92l%91%E5%8D%E3%8Es and try to decode with UTF-8 encoding
            arg = new HTTPArgument("name.?", encodedValue, true, "UTF-8");
            assertNotEquals("HTTPArgument(\"name.?\", encodedValue, true, \"UTF-8\").getValue()", value, arg.getValue());
        }

        private void testEncodings(String encoding) {
            HTTPArgument arg;
            arg = new HTTPArgument("name.?", "value_ here", false, encoding);
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", false, encoding).getName()", "name.?", arg.getName());
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", false, encoding).getValue()", "value_ here", arg.getValue());
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", false, encoding).getEncodedName()", "name.%3F", arg.getEncodedName());
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", false, encoding).getValue()", "value_+here", arg.getEncodedValue());
            // Show that can bypass encoding:
            arg.setAlwaysEncoded(false);
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"name.?\", \"value_ here\", false, encoding).getEncodedName()"
                + " should not be encoded", "name.?", arg.getEncodedName());
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"name.?\", \"value_ here\", false, encoding).getValue()"
                + " should not be encoded", "value_ here", arg.getEncodedValue());

            // When the name and value parameter does not have valid encoded value (Encoded
            // and Decoded values are same)
            // In real encoded value for "name.?" is "name.%3F" and the encoded value for
            // "value_here" is "value_+here"
            arg = new HTTPArgument("name.?", "value_ here", true, encoding);
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", true, encoding).getName()", "name.?", arg.getName());
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", true, encoding).getValue()", "value_ here", arg.getValue());
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", true, encoding).getEncodedName()", "name.%3F", arg.getEncodedName());
            assertEquals("HTTPArgument(\"name.?\", \"value_ here\", true, encoding).getValue()", "value_+here", arg.getEncodedValue());
            arg.setAlwaysEncoded(false); // by default, name/value are encoded on fetch
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"name.?\", \"value_ here\", true, encoding).getEncodedName()"
                + " should not be encoded", "name.?", arg.getEncodedName());
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"name.?\", \"value_ here\", true, encoding).getValue()"
                + " should not be encoded", "value_ here", arg.getEncodedValue());

            // Try a real encoded argument
            arg = new HTTPArgument("name.%3F", "value_+here", true, encoding);
            assertEquals("HTTPArgument(\"name.%3F\", \"value_+here\", true, encoding).getName()", "name.?", arg.getName());
            assertEquals("HTTPArgument(\"name.%3F\", \"value_+here\", true, encoding).getValue()", "value_ here", arg.getValue());
            assertEquals("HTTPArgument(\"name.%3F\", \"value_+here\", true, encoding).getEncodedName()", "name.%3F", arg.getEncodedName());
            assertEquals("HTTPArgument(\"name.%3F\", \"value_+here\", true, encoding).getValue()", "value_+here", arg.getEncodedValue());
            // Show that can bypass encoding:
            arg.setAlwaysEncoded(false);
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"name.%3F\", \"value_+here\", true, encoding).getEncodedName()"
                + " should not be encoded", "name.?", arg.getEncodedName());
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"name.%3F\", \"value_+here\", true, encoding).getValue()"
                + "should not be encoded", "value_ here", arg.getEncodedValue());

            arg = new HTTPArgument("", "\00\01\07", "", false, encoding);
            arg.setAlwaysEncoded(false);
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"\", \"\00\01\07\", \"\", false, encoding).getEncodedName()"
                + " should not be encoded", "", arg.getEncodedName());
            assertEquals("setAlwaysEncoded=false => HTTPArgument(\"\", \"\00\01\07\", \"\", false, encoding).getValue()"
                + " should not be encoded", "\00\01\07", arg.getEncodedValue());
        }

}
