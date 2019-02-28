package com.licheedev.serialtool.comn.send;

public class SerialData {

    public byte cmd;
//    // 计算CRC32
//    kal_uint32 hwtool_compute_crc32(kal_uint8 *pData, kal_uint32 Length)
//    {
//        kal_uint32 xbit;
//        kal_uint32 data;
//        kal_uint32 mycrc = 0xFFFFFFFF;
//        kal_uint32 bits;
//        kal_uint32 len = Length;
//
//        while(len)
//        {
//            xbit = 1U << 31;
//
//            if((len / 4) > 0)
//            {
//                data = *pData++;
//                data <<= 8;
//                data += *pData++;
//                data <<= 8;
//                data += *pData++;
//                data <<= 8;
//                data += *pData++;
//                len -= 4;
//            }
//            else
//            {
//                data = 0;
//
//                if(len == 3)
//                {
//                    data = *pData++;
//                    data <<= 8;
//                    data += *pData++;
//                    data <<= 8;
//                    data += *pData++;
//                    data <<= 8;
//                }
//
//                if(len == 2)
//                {
//                    data = *pData++;
//                    data <<= 8;
//                    data += *pData++;
//                    data <<= 16;
//                }
//
//                if(len == 1)
//                {
//                    data = *pData++;
//                    data <<= 24;
//                }
//
//                len = 0;
//            }
//
//            for(bits = 0; bits < 32; bits++)
//            {
//                if(mycrc & 0x80000000)
//                {
//                    mycrc <<= 1;
//                    mycrc ^= 0x04c11db7;
//                }
//                else
//                {
//                    mycrc <<= 1;
//                }
//
//                if(data & xbit)
//                {
//                    mycrc ^= 0x04c11db7;
//                }
//
//                xbit >>= 1;
//            }
//        }
//
//        return mycrc;
//    }


    public static int byteToInt(byte b) {
        return b&0x0FF;
    }

    public static long getCrc(byte[] bytes, int len) {
        long xbit;
        long data;
        long mycrc = 0x00000000ffffffffL;
        long bits;

        int i = 0;

        while(len > 0)
        {
            xbit = 0x0000000080000000L;

            if(len >= 4)
            {
                data = byteToInt(bytes[i]);
                i++;
                data <<= 8;
                data = data & 0x00000000ffffffffL;
                data += byteToInt(bytes[i]);
                i++;
                data <<= 8;
                data = data & 0x00000000ffffffffL;
                data += byteToInt(bytes[i]);
                i++;
                data <<= 8;
                data = data & 0x00000000ffffffffL;
                data += byteToInt(bytes[i]);
                i++;
                len -= 4;
            }
            else
            {
                data = 0;

                if(len == 3)
                {
                    data += byteToInt(bytes[i]);
                    i++;
                    data <<= 8;
                    data = data & 0x00000000ffffffffL;
                    data += byteToInt(bytes[i]);
                    i++;
                    data <<= 8;
                    data = data & 0x00000000ffffffffL;
                    data += byteToInt(bytes[i]);
                    i++;
                    data <<= 8;
                    data = data & 0x00000000ffffffffL;
                }

                if(len == 2)
                {
                    data += byteToInt(bytes[i]);
                    i++;
                    data <<= 8;
                    data = data & 0x00000000ffffffffL;
                    data += byteToInt(bytes[i]);
                    i++;
                    data <<= 16;
                    data = data & 0x00000000ffffffffL;
                }

                if(len == 1)
                {
                    data += byteToInt(bytes[i]);
                    i++;
                    data <<= 24;
                    data = data & 0x00000000ffffffffL;
                }

                len = 0;
            }

            for(bits = 0; bits < 32; bits++)
            {
                if((mycrc & 0x0000000080000000L) != 0)
                {
                    mycrc <<= 1;
                    mycrc = mycrc & 0x00000000ffffffffL;
                    mycrc ^= 0x0000000004c11db7L;
                    mycrc = mycrc & 0x00000000ffffffffL;
                }
                else
                {
                    mycrc <<= 1;
                    mycrc = mycrc & 0x00000000ffffffffL;
                }

                if((data & xbit) != 0)
                {
                    mycrc ^= 0x0000000004c11db7L;
                    mycrc = mycrc & 0x00000000ffffffffL;
                }

                xbit >>= 1;
            }
        }

        return mycrc & 0x00000000ffffffffL;
    }
}
