package com.study.pingindemo.utils;

/**
 * Created by hcw on 2019/3/7.
 * Copyright©hcw.All rights reserved.
 */

import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author        hcw
 * @time          2019/3/7 21:19
 * @description  将 assets 目录下的 File 文件拷贝到 app 缓存目录
*/

public class FileUtils {
    public static String copyAssetsAndWrite(Context context,String fileName){
        try {
            File cacheDir = context.getCacheDir();
            if (!cacheDir.exists()){
                cacheDir.mkdirs();
            }
            File outFile = new File(cacheDir,fileName);
            if (!outFile.exists()){
                boolean res = outFile.createNewFile();
                if (res){
                    InputStream is = context.getAssets().open(fileName);
                    FileOutputStream fos = new FileOutputStream(outFile);
                    byte[] buffer = new byte[is.available()];
                    int byteCount;
                    while ((byteCount = is.read(buffer)) != -1){
                        fos.write(buffer,0,byteCount);
                    }
                    fos.flush();
                    is.close();
                    fos.close();
                    Toast.makeText(context,"downlaod success!",Toast.LENGTH_SHORT).show();
                    return outFile.getAbsolutePath();
                }else {
                    Toast.makeText(context,"文件已经存在",Toast.LENGTH_SHORT).show();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return "";
    }
}
