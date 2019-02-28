package com.licheedev.serialtool.comn;

import android.os.HandlerThread;
import android.serialport.SerialPort;
import android.util.Log;

import com.licheedev.myutils.LogPlus;
import com.licheedev.serialtool.comn.send.SerialSendData;
import com.licheedev.serialtool.comn.message.LogManager;
import com.licheedev.serialtool.comn.message.SendMessage;
import com.licheedev.serialtool.comn.send.SerialSendGetDeviceInfoData;
import com.licheedev.serialtool.comn.send.SerialSendLockData;
import com.licheedev.serialtool.comn.send.SerialSendUnlockData;
import com.licheedev.serialtool.util.ByteUtil;
import com.licheedev.serialtool.util.ByteUtilZZT;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/3/28 0028.
 */
public class SerialPortManager {

    private static final String TAG = "SerialPortManager";

    private SerialReadThread mReadThread;
    private OutputStream mOutputStream;
    private HandlerThread mWriteThread;
    private Scheduler mSendScheduler;

    private static class InstanceHolder {

        public static SerialPortManager sManager = new SerialPortManager();
    }

    public static SerialPortManager instance() {
        return InstanceHolder.sManager;
    }

    private SerialPort mSerialPort;

    private SerialPortManager() {
    }

    /**
     * 打开串口
     *
     * @param device
     * @return
     */
    public SerialPort open(Device device) {
        return open(device.getPath(), device.getBaudrate());
    }

    /**
     * 打开串口
     *
     * @param devicePath
     * @param baudrateString
     * @return
     */
    public SerialPort open(String devicePath, String baudrateString) {

        sendCmdCb300();

        if (mSerialPort != null) {
            close();
        }

        try {
            File device = new File(devicePath);
            // zzz 打开串口
//            File device = new File("/dev/rfcomm");
//            File device = new File("/dev/ttyGS");
//            File device = new File("/dev/ttyUSB");
//            File device = new File("/dev/ttyS");
//            File device = new File("/dev/ttyGS3");
//            File device = new File("/dev/ttyGS2");
//            File device = new File("/dev/ttyGS1");
//            File device = new File("/dev/ttyGS0");
//            File device = new File("/dev/ttyS3");
//            File device = new File("/dev/ttyS2");
//            File device = new File("/dev/ttyS1");
//            File device = new File("/dev/ttyS0");

            int baurate = Integer.parseInt(baudrateString);
            mSerialPort = new SerialPort(device, baurate, 0);

            mReadThread = new SerialReadThread(mSerialPort.getInputStream());
            mReadThread.start();

            mOutputStream = mSerialPort.getOutputStream();

            mWriteThread = new HandlerThread("write-thread");
            mWriteThread.start();
            mSendScheduler = AndroidSchedulers.from(mWriteThread.getLooper());

            return mSerialPort;
        } catch (Throwable tr) {
            LogPlus.e(TAG, "打开串口失败", tr);
            close();
            return null;
        }
    }

    /**
     * 关闭串口
     */
    public void close() {
        if (mReadThread != null) {
            mReadThread.close();
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mWriteThread != null) {
            mWriteThread.quit();
        }

        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    /**
     * 发送数据
     *
     * @param datas
     * @return
     */
    private void sendData(byte[] datas) throws Exception {
        mOutputStream.write(datas);
        mReadThread.getData(false , null , 0);
        mReadThread.getData(true , null , 0);
    }

    /**
     * (rx包裹)发送数据
     *
     * @param datas
     * @return
     */
    private Observable<Object> rxSendData(final byte[] datas) {

        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                try {
                    sendData(datas);
                    emitter.onNext(new Object());
                } catch (Exception e) {

                    LogPlus.e("发送：" + ByteUtil.bytes2HexStr(datas) + " 失败", e);

                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                        return;
                    }
                }
                emitter.onComplete();
            }
        });
    }

    /**
     * 发送命令包
     */
    public void sendCommand(final String command) {

        // TODO: 2018/3/22
        LogPlus.i("发送命令：" + command);

//        byte[] bytes = ByteUtil.hexStr2bytes(command);

        byte[] bytes = new byte[]{0x01, 0x03 , 0x00 , 0x01 ,  0x00 , 0x04 , 0x15, (byte) 0xc9 } ;

        rxSendData(bytes).subscribeOn(mSendScheduler).subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object o) {
                LogManager.instance().post(new SendMessage(command));
            }

            @Override
            public void onError(Throwable e) {
                LogPlus.e("发送失败", e);
            }

            @Override
            public void onComplete() {

            }
        });
    }

    public static final byte GET_DEVICE_INFO = 1;

    public static final byte SOFT_LOCK = 2;
    public static final byte SOFT_UNLOCK = 3;

    public byte[] sendCmd(byte cmd) {
        // send
        Map<Byte, Class> sendClassMap = new HashMap<>();
        sendClassMap.put(GET_DEVICE_INFO, SerialSendGetDeviceInfoData.class);
        sendClassMap.put(SOFT_LOCK, SerialSendLockData.class);
        sendClassMap.put(SOFT_UNLOCK, SerialSendUnlockData.class);

        Class sendCls = sendClassMap.get(cmd);
        byte[] datas = null ;
        try {
            SerialSendData resObj;
            resObj = (SerialSendData)sendCls.newInstance();
            resObj.init(cmd);
            datas = resObj.getBytes();

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return datas ;
    }


    public static short FindCRC(byte[] data){
        short CRC=0;
        short genPoly = (short) 0xFFFF;
        for(int i=0;i<data.length; i++){
            CRC ^= data[i];
            for(int j=0;j<8;j++){
                if((CRC & 0x80) != 0){
                    CRC = (short) ((CRC << 1) ^ genPoly);
                }else{
                    CRC <<= 1;
                }
            }
        }
        CRC &= 0xff;//保证CRC余码输出为2字节。
        return CRC;
    }

    /**
     *  cb300 读取
     * @return
     */
    public byte[] sendCmdCb300 () {
        byte[] byteQuery = new byte[8] ;
        //NODE
        byte[] b1 = new byte[1];
//        b1 = ByteUtilZZT.intToByteArrayBig(1) ;
        b1[0] = (byte) 1 ;
//       FUN
        byte[] b2 = new byte[1];
//        b2 = ByteUtilZZT.intToByteArrayBig(3) ;
        b2[0] =  (byte) 3  ;
//       ID(start)
        byte[] b3 = new byte[2];
//        b3 = ByteUtilZZT.intToByteArrayBig(11) ;
        b3[1] = (byte) 12 ;
//       NR
        byte[] b4 = new byte[2];
//        b4 = ByteUtilZZT.intToByteArrayBig(2) ;
        b4[1] =  (byte) 2 ;
//       CRC
        byte[] b5 = new byte[2];
        b5 = ByteUtilZZT.intToByteArrayBig(2) ;

        System.arraycopy(b1, 0, byteQuery, 0, 1);
        System.arraycopy(b2, 0, byteQuery, 1, 1);
        System.arraycopy(b3, 0, byteQuery, 2, 2);
        System.arraycopy(b4, 0, byteQuery, 4, 2);
        System.arraycopy(b5, 0, byteQuery, 6, 2);

        byte[] byteQueryCb = new byte[]{0x01, 0x03 , 0x00 , 0x0c ,  0x00 , 0x03 , (byte) 0xc5, (byte) 0xc8} ;
        byte[] byteData = new byte[6] ;

        System.arraycopy(byteQueryCb, 0, byteData, 0, 6);
//        short crc = FindCRC(byteData);
        short crc = 0 ;
        short ii = (short) CRC16Util.calcCrc16(byteData);
        byte[]  bii = ByteUtilZZT.intToByteArrayBig(ii) ;
        byte[]  bii1 = ByteUtilZZT.intToByteArrayLittel(ii) ;
        byte[] byteCrc = ByteUtilZZT.shortToByteArrayBig(ii) ;
        byte[] byteCrcL = ByteUtilZZT.shortToByteArrayLittel(ii) ;

        String stringCrc = ByteUtilZZT.bytesToHex2(byteCrc) ;
        String stringCrc1 = ByteUtil.bytes2HexStr(byteCrc) ;
        Log.d(TAG, "验证的crc:" + stringCrc );

        return byteQueryCb ;
    }


    int dataLen = 0 ;
    public void sendCommand() {

        // TODO: 2018/3/22
        LogPlus.i("发送命令：" );

//        byte[] bytes = sendCmd( GET_DEVICE_INFO );
//        byte[] bytes = sendCmdCb300() ;
//        final byte[] bytes = new byte[]{0x01, 0x03 , 0x00 , 0x01 ,  0x00 , 0x04 , 0x15, (byte) 0xc9 } ;
        final byte[] bytes = new byte[]{0x01, 0x03 , 0x00 , 0x0c ,  0x00 , 0x03 , (byte) 0xc5, (byte) 0xc8} ;

        dataLen = bytes.length ;

        rxSendData(bytes).subscribeOn(mSendScheduler).subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object o) {
                LogManager.instance().post(new SendMessage("sendSerialPort: 发送数据, len:" + dataLen + "  " + ByteUtil.bytes2HexStr(bytes) ));
            }

            @Override
            public void onError(Throwable e) {
                LogPlus.e("发送失败", e);
            }

            @Override
            public void onComplete() {

            }
        });
    }
}
