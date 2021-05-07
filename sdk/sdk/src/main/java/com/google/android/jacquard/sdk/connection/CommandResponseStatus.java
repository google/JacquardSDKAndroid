/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.connection;

/** Map status codes to errors */
public enum CommandResponseStatus {

  OK(0),

  /** The domain or opcode is unsupported. */
  ERROR_UNSUPPORTED(1),

  /** The parameters to this command were incorrect/invalid. */
  ERROR_BAD_PARAM(2),

  /** The device has a critically low battery and will not execute the command. */
  ERROR_BATTERY(3),

  /** We have experienced a failure in some hardware component. */
  ERROR_HARDWARE(4),

  /**
   * The key in an authentication call was incorrect, or there has been  no authentication yet and 
   * this call must happen on an authenticated connection.
   */
  ERROR_AUTH(5),

  /** The device has an invalid device type. */
  ERROR_DEVICE_TYPE_INFO(6),

  /** Invalid state to perform requested operation. */
  ERROR_INVALID_STATE(7),

  /** Error accessing Flash for either read/write or erase operation request. */
  ERROR_FLASH_ACCESS(8),

  /** Checksum error. */
  ERROR_CHECKSUM(9),

  /** Error Busy - e.g. Busy updating Interposer FW. */
  ERROR_BUSY(10),

  /** Error Generated in APP only */
  ERROR_APP_TIMEOUT(253),

  /** Error Generated in APP only */
  ERROR_APP_UNKNOWN(254),

  /** Some internal, unknown error has occurred. */
  ERROR_UNKNOWN(255);

  private final int statusCode;

  CommandResponseStatus(int statusCode) {
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  /** Creates a {@link Throwable} from a status code. */
  public static Throwable from(int statusCode) {
    CommandResponseStatus error = CommandResponseStatus.ERROR_UNKNOWN;
    for (CommandResponseStatus status : CommandResponseStatus.values()) {
      if (status.statusCode == statusCode) {
        error = status;
        break;
      }
    }
    return new Exception(error.toString());
  }
}
