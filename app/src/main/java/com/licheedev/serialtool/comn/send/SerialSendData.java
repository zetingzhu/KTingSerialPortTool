package com.licheedev.serialtool.comn.send;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SerialSendData extends SerialData {

    protected final byte START_FLAG = 0x55;

    protected final int PKG_LEN = 18;



    public int timeStamp;

    public void init(byte cmd) {
        this.cmd = cmd;
    }


    // 获取8字节内容
//    有的命令有详细内容，有的没有。没有的情况下设置为0。
//     0x04	蜂鸣器控制：第一字节表示鸣叫次数，第二字节表示鸣叫间隔
//     0x1f	"西游锁车，第1个字节表示锁车后是否要延时关闭控制器。为1要关闭，为0不关闭
//    第2,3字节表示延时的时间，单位为秒，小端模式。
//    西游解锁目前没有带参数。"
//            12	"字节1：刹车自检 : 0=不启动自检，1=启动自检
//    字节2：转把自检 : 0=不启动自检，1=启动自检
//    字节3：助力自检 : 0=不启动自检，1=启动自检
//    字节4：电机运行自检 : 0=不启动自检，1=启动自检"
    public byte[] getContent() {
        // can be overrided by sub class
        byte[] bytes = {0, 0, 0, 0, 0, 0, 0, 0};
        return bytes;
    }


    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(PKG_LEN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(START_FLAG);
        buffer.put(cmd);
        buffer.put(getContent());
        buffer.putInt(timeStamp);
        int crc = (int)getCrc(buffer.array(), PKG_LEN - 4);
        buffer.putInt(crc);


        return buffer.array();


    }
}
