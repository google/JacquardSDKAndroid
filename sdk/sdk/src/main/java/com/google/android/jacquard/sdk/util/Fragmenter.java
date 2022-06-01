/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.util;

import com.google.android.jacquard.sdk.log.PrintLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Class for decomposing and decoding byte data received from the jacquard tag. */
public class Fragmenter {

  private final String tag;
  private int mtuSize;

  public Fragmenter(String name, int mtuSize) {
    this.mtuSize = mtuSize; // maximum transmission unit size.
    tag = Fragmenter.class.getSimpleName() + "-" + name;
  }

  void setMtu(int mtu) {
    this.mtuSize = mtu;
  }

  /**
   * Maximum message size.
   */
  static final int MAX_MESSAGE_LENGTH = 1024;

  /**
   * Indicates that the fragment is the first of the sequence.
   */
  static final int FIRST_FRAGMENT_FLAG = 0x80;

  /**
   * Indicates that the fragment is the last of the sequence.
   */
  static final int LAST_FRAGMENT_FLAG = 0x40;

  /**
   * Holds the next expected fragment counter.
   */
  private int currentFragmentCounter = 0;

  /**
   * Holds the expected message length. This is encoded in the first fragment.
   */
  private int currentMessageLength = 0;

  /**
   * The message buffer.
   */
  private final ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream(MAX_MESSAGE_LENGTH);

  /**
   * Class for parsing/generating protocol buffer compatible VarInts.
   */
  public static class VarInt {

    public byte[] data = null;
    public int value = 0;
    public int length = 0;

    public VarInt(byte[] data) {
      this.data = data;
      decode();
    }

    public VarInt(int value) {
      this.value = value;
      encode();
    }

    private void decode() {
      value = 0;
      int shift = 0;

      int i = 0;
      while (i < data.length) {
        // Cast, avoiding sign-extension.
        int tmp = ((int) data[i++] & 0xFF);

        // Shift and add next seven bits.
        value |= (tmp & 0x7F) << shift;
        shift += 7;

        // Last chunk?
        if ((tmp & 0x80) == 0) {
          break;
        }
      }

      length = i;
    }

    private void encode() {
      ByteArrayOutputStream buf = new ByteArrayOutputStream(4);

      while (true) {
        if ((value & ~0x7F) == 0) {
          // Last group, write the bits and stop.
          buf.write(value & 0xFF);
          break;
        } else {
          // Write out the next 7 bits with MSB set to indicate more to come.
          buf.write((value & 0x7F) | 0x80);
          value = value >>> 7;
        }
      }

      data = buf.toByteArray();
      length = data.length;
    }
  }

  /**
   * Decomposes a message into fragments.
   *
   * @param message The message to fragment.
   * @return An {@link ArrayList} of fragments.
   */
  public List<byte[]> fragmentData(byte[] message) {
    int maxFragmentPayloadLength = mtuSize - 3; // 1 byte opcode, 2 bytes att handle
    ArrayList<byte[]> fragments = new ArrayList<>();

    if (message.length > MAX_MESSAGE_LENGTH) {
      PrintLogger.e(
          Fragmenter.class.getSimpleName(),
          "Message larger than the maximum allowable size (1024 bytes)");
      return null;
    }

    VarInt encodedLength = new VarInt(message.length);

    ByteArrayInputStream input = new ByteArrayInputStream(message);
    ByteArrayOutputStream output = new ByteArrayOutputStream(maxFragmentPayloadLength);

    byte counter = 0;
    int nextDataLen = 0;
    byte[] nextData = new byte[maxFragmentPayloadLength];

    while (input.available() > 0) {
      byte flow = counter;
      int payloadLength = maxFragmentPayloadLength;

      // Compose and write the segment header.
      if (counter == 0) {
        flow = (byte) (flow | FIRST_FRAGMENT_FLAG);
        payloadLength -= encodedLength.length;
      }
      payloadLength -= 1;  // 1 byte fragment header
      if (input.available() <= payloadLength) {
        flow = (byte) (flow | LAST_FRAGMENT_FLAG);
      }
      output.write(flow);

      if (counter == 0) {
        // Write the encoded length as the first part of the first fragment.
        output.write(encodedLength.data, 0, encodedLength.length);
      }

      // Retrieve and write the maximum available bytes for the payload up to the end of the
      // message.
      nextDataLen = input.read(nextData, 0, payloadLength);
      output.write(nextData, 0, nextDataLen);

      fragments.add(output.toByteArray());
      output.reset();

      ++counter;
    }

    return fragments;
  }

  /**
   * Decodes a message fragment returning the message if it is complete.
   *
   * <p>Note: This method will validate the fragment number and message length. If fragments are
   * received out of sequence of the final message size does not match the reported size, then the
   * message state will be reset.
   *
   * @return The complete message if the given fragment is valid and the last in the sequence, null
   * otherwise.
   */
  public byte[] decodeFragment(byte[] fragment) {
    byte[] result = null;
    int payloadOffset = 1;

    int fragmentCounter = fragment[0] & 0xF;
    if ((fragment[0] & FIRST_FRAGMENT_FLAG) != 0) {
      VarInt encoded = new VarInt(Arrays.copyOfRange(fragment, 1, 5));
      currentMessageLength = encoded.value;
      currentFragmentCounter = (fragmentCounter + 1) & 0xF;
      payloadOffset += encoded.length;
    } else {
      if (fragmentCounter != currentFragmentCounter) {
        PrintLogger.e(
            tag,
            String.format(
                "Fragment counter does not match expected (%d != %d)",
                fragmentCounter, currentFragmentCounter));
        reset();
        return null;
      }
      currentFragmentCounter = (currentFragmentCounter + 1) & 0xF;
    }

    messageBuffer.write(fragment, payloadOffset, fragment.length - payloadOffset);

    if ((fragment[0] & LAST_FRAGMENT_FLAG) != 0) {
      if (messageBuffer.size() != currentMessageLength) {
        PrintLogger.e(
            tag,
            String.format(
                "Invalid message size! Expected %d bytes but received %d bytes",
                currentMessageLength, messageBuffer.size()));
      } else {
        result = messageBuffer.toByteArray();
      }
      reset();
    }

    return result;
  }

  /**
   * Resets the message state.
   */
  public void reset() {
    currentFragmentCounter = 0;
    currentMessageLength = 0;
    messageBuffer.reset();
  }
}