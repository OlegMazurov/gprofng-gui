/* Copyright (C) 2022 Free Software Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses>.  */


/**
 * <Header, ResponseBody> where
 *
 * <p>Header <HeaderMarker, RequestID, ResponseType, ResponseStatus, nBytes>
 *
 * <p>HeaderMarker (1 byte, value=255) RequestID (4 bytes) ResponseType (INTEGER): (1 byte) o 0: ACK
 * o 1: PROGRESS o 2: COMPLETE o 3: HANDSHAKE ResponseStatus: (1 byte) o 0: DEFAULT, IGNORE o 1:
 * SUCCESS o 2: FAILURE o 3: CANCELED -or- o VersionID in case of Handshake requests nBytes: Number
 * of bytes in the response body (4 bytes)
 *
 * <p>ResponseBody string containing the final result or intermediate status details
 */
package org.gprofng.mpmt.ipc;

class IPCResponseHeader {
  protected static final int headerSizeInBytes = 11;

  protected enum ResponseType {
    ACK(0),
    PROGRESS(1),
    COMPLETE(2),
    HANDHAKE(3);
    private final int value;

    private ResponseType(int value) {
      this.value = value;
    }

    protected int value() {
      return value;
    }

    protected static ResponseType fromValue(int val) {
      for (ResponseType rt : ResponseType.values()) {
        if (rt.value() == val) {
          return rt;
        }
      }
      return null;
    }
  };

  private int requestID;
  private ResponseType responseType;
  private IPCResult.CC responseStatus;
  private int nBytes;

  protected IPCResponseHeader(
      int requestID, ResponseType responseType, IPCResult.CC responseStatus, int nBytes) {
    this.requestID = requestID;
    this.responseType = responseType;
    this.responseStatus = responseStatus;
    this.nBytes = nBytes;
  }

  /**
   * @return the requestID
   */
  protected int getRequestID() {
    return requestID;
  }

  /**
   * @return the responseType
   */
  protected ResponseType getResponseType() {
    return responseType;
  }

  /**
   * @return the responseStatus
   */
  protected IPCResult.CC getResponseStatus() {
    return responseStatus;
  }

  /**
   * @return the nBytes
   */
  protected int getnBytes() {
    return nBytes;
  }
}
