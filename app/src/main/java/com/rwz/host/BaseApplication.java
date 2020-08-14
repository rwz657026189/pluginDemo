package com.rwz.host;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

/**
 * date： 2020/8/12 16:10
 * author： rwz
 * description：
 **/
public class BaseApplication extends Application {

    private static final String TAG = "BaseApplication";
    private DexClassLoader mPluginClassLoader;
    private static BaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            apply(this);
            HookUtil.hookIntentBefore(this);
            HookUtil.hookIntentAfter(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BaseApplication getInstance() {
        return instance;
    }

    public DexClassLoader getPluginClassLoader() {
        return mPluginClassLoader;
    }

    public static final String dexPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "plugin.apk";
    private void apply(Context context) throws Exception{
        //获取宿主dexElements
        ClassLoader hostClassLoader = context.getClassLoader();
        Object hostPathList = ReflectUtil.getValue(BaseDexClassLoader.class, hostClassLoader, "pathList");
        Object[] hostDexElements = (Object[]) ReflectUtil.getValue(hostPathList, "dexElements");
        //获取插件dexElements
        String fileReleasePath = getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();
        Log.d(TAG, "apply: " + getCacheDir().getAbsolutePath() + ", hostClassLoader = " + hostClassLoader);
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器
        mPluginClassLoader = new DexClassLoader(dexPath, fileReleasePath, null, hostClassLoader);
        Object pluginPathList = ReflectUtil.getValue(BaseDexClassLoader.class, mPluginClassLoader, "pathList");
        Object[] pluginDexElements = (Object[]) ReflectUtil.getValue(pluginPathList, "dexElements");
        //合并dexElements
        Object[] totalDexElements = (Object[]) Array.newInstance(hostDexElements.getClass().getComponentType(),
                hostDexElements.length + pluginDexElements.length);
        System.arraycopy(hostDexElements, 0, totalDexElements, 0, hostDexElements.length);
        System.arraycopy(pluginDexElements, 0, totalDexElements, hostDexElements.length, pluginDexElements.length);
        Log.d(TAG, "apply: " + hostDexElements.length);
        Log.d(TAG, "apply: " + pluginDexElements.length);
        Log.d(TAG, "apply: " + totalDexElements.length);
        //更新宿主dexElements
        ReflectUtil.setValue(hostPathList, "dexElements", totalDexElements);
        Log.d(TAG, "apply: success, " + hostClassLoader);
    }

}
