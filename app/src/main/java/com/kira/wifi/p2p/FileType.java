package com.kira.wifi.p2p;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import static android.content.ContentValues.TAG;

public class FileType {

    public static final HashMap<String, String> mFileTypes = new HashMap<String, String>();

    static  {
        mFileTypes.put("ffd8ffe000104a464946", "jpg"); //JPEG (jpg)
        mFileTypes.put("ffd8ffe1250f45786966", "jpg"); //JPEG (jpg)
        mFileTypes.put("89504e470d0a1a0a0000", "png"); //PNG (png)
        mFileTypes.put("47494638396126026f01", "gif"); //GIF (gif)
        mFileTypes.put("49492a00227105008037", "tif"); //TIFF (tif)
        mFileTypes.put("424d228c010000000000", "bmp"); //16色位图(bmp)
        mFileTypes.put("424d8240090000000000", "bmp"); //24位位图(bmp)
        mFileTypes.put("424d8e1b030000000000", "bmp"); //256色位图(bmp)
        mFileTypes.put("41433130313500000000", "dwg"); //CAD (dwg)
        mFileTypes.put("3c21444f435459504520", "html"); //HTML (html)
        mFileTypes.put("3c21646f637479706520", "htm"); //HTM (htm)
        mFileTypes.put("48544d4c207b0d0a0942", "css"); //css
        mFileTypes.put("696b2e71623d696b2e71", "js"); //js
        mFileTypes.put("7b5c727466315c616e73", "rtf"); //Rich Text Format (rtf)
        mFileTypes.put("38425053000100000000", "psd"); //Photoshop (psd)
        mFileTypes.put("46726f6d3a203d3f6762", "eml"); //Email [Outlook Express 6] (eml)
        mFileTypes.put("d0cf11e0a1b11ae10000", "doc"); //MS Excel 注意：word、msi 和 excel的文件头一样
        mFileTypes.put("d0cf11e0a1b11ae10000", "vsd"); //Visio 绘图
        mFileTypes.put("5374616E64617264204A", "mdb"); //MS Access (mdb)
        mFileTypes.put("252150532D41646F6265", "ps");
        mFileTypes.put("255044462d312e350d0a", "pdf"); //Adobe Acrobat (pdf)
        mFileTypes.put("2e524d46000000120001", "rmvb"); //rmvb/rm相同
        mFileTypes.put("464c5601050000000900", "flv"); //flv与f4v相同
        mFileTypes.put("00000020667479706d70", "mp4");
        mFileTypes.put("00000020667479706973", "mp4");
        mFileTypes.put("00000018667479706D70", "mp4");
        mFileTypes.put("49443303000000002176", "mp3");
        mFileTypes.put("000001ba210001000180", "mpg"); //
        mFileTypes.put("3026b2758e66cf11a6d9", "wmv"); //wmv与asf相同
        mFileTypes.put("52494646e27807005741", "wav"); //Wave (wav)
        mFileTypes.put("52494646d07d60074156", "avi");
        mFileTypes.put("4d546864000000060001", "mid"); //MIDI (mid)
        mFileTypes.put("504b0304140000000800", "zip");
        mFileTypes.put("526172211a0700cf9073", "rar");
        mFileTypes.put("235468697320636f6e66", "ini");
        mFileTypes.put("504b03040a0000000000", "jar");
        mFileTypes.put("4d5a9000030000000400", "exe");//可执行文件
        mFileTypes.put("3c25402070616765206c", "jsp");//jsp文件
        mFileTypes.put("4d616e69666573742d56", "mf");//MF文件
        mFileTypes.put("3c3f786d6c2076657273", "xml");//xml文件
        mFileTypes.put("494e5345525420494e54", "sql");//xml文件
        mFileTypes.put("7061636b616765207765", "java");//java文件
        mFileTypes.put("406563686f206f66660d", "bat");//bat文件
        mFileTypes.put("1f8b0800000000000000", "gz");//gz文件
        mFileTypes.put("6c6f67346a2e726f6f74", "properties");//bat文件
        mFileTypes.put("cafebabe0000002e0041", "class");//bat文件
        mFileTypes.put("49545346030000006000", "chm");//bat文件
        mFileTypes.put("04000000010000001300", "mxp");//bat文件
        mFileTypes.put("504b0304140006000800", "docx");//docx文件
        mFileTypes.put("d0cf11e0a1b11ae10000", "wps");//WPS文字wps、表格et、演示dps都是一样的
        mFileTypes.put("6431303a637265617465", "torrent");


        mFileTypes.put("6d6f6f76", "mov"); //Quicktime (mov)
        mFileTypes.put("ff575043", "wpd"); //WordPerfect (wpd)
        mFileTypes.put("cfad12feC5fd746f", "dbx"); //Outlook Express (dbx)
        mFileTypes.put("2142444e", "pst"); //Outlook (pst)
        mFileTypes.put("ac9ebd8f", "qdf"); //Quicken (qdf)
        mFileTypes.put("e3828596", "pwl"); //Windows Password (pwl)
        mFileTypes.put("2e7261fd", "ram"); //Real Audio (ram)
        mFileTypes.put("null", null); //null
    }

    public static String getFileType(FileInputStream in) {
        String keySearch = getFileHeader(in).toLowerCase();
        String fileSuffix = mFileTypes.get(keySearch);

        if(TextUtils.isEmpty(fileSuffix)){
            Iterator<String> keyList = mFileTypes.keySet().iterator();
            String key, keySearchPrefix = keySearch.substring(0,5);
            while (keyList.hasNext()){
                key = keyList.next().toLowerCase();
                if(key.contains(keySearchPrefix)) {
                    fileSuffix = mFileTypes.get(key);
                    break;
                }
            }
        }
        return fileSuffix;
    }

    public static String getFileHeader(FileInputStream in) {

        String value = null;

        try {
            byte[] b = new byte[10];

            in.read(b, 0, b.length);

            value = bytesToHexString(b);

        } catch (Exception e) {

        } finally {

            if (null != in) {

                try {

                    in.close();

                } catch (IOException e) {
                }

            }

        }

        return value;

    }

    private static String bytesToHexString(byte[] src) {

        StringBuilder builder = new StringBuilder();

        if (src == null || src.length <= 0) {

            return null;

        }

        String hv;

        for (int i = 0; i < src.length; i++) {

            hv = Integer.toHexString(src[i] & 0xFF).toUpperCase();

            if (hv.length() < 2) {

                builder.append(0);

            }

            builder.append(hv);

        }

        return builder.toString();

    }

}