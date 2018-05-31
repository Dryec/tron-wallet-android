package org.tron.core.config;

public interface Parameter {

  interface CommonConstant {
    byte ADD_PRE_FIX_BYTE = (byte) 0x41;   //a0 + address  ,a0 is version
    String ADD_PRE_FIX_STRING = "41";
    int ADDRESS_SIZE = 21;
    int BASE58CHECK_ADDRESS_SIZE = 35;
  }

}
