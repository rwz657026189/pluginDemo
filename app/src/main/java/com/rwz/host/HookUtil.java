package com.rwz.host;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import androidx.annotation.NonNull;

/**
 * date： 2020/8/14 10:54
 * author： rwz
 * description：
 **/
public class HookUtil {

    private static final String TAG = "HookUtil";
    public static final String KEY_TARGET_INTENT = "KEY_TARGET_INTENT";

    //https://mp.weixin.qq.com/s?src=11&timestamp=1597375449&ver=2521&signature=QiAy1ppfbF-jvEJs2rU0I2lvQwAXzdOw7GLPjm0D*DIBPRyXEQ-i7d0i0zlIE1vfQYOW3l9zPk6CBLS2uWlNA8YDGayneRAwbD-SRnbjVKLoSOJwpS83nWkCO419M4Yp&new=1
    //Android插件化原理之Activity插件化
    public static void hookIntentBefore(Context context) throws Exception {
        ClassLoader classLoader = context.getClassLoader();
        Class<?> activityManagerCls = classLoader.loadClass("android.app.ActivityManager");
        Proxy.getProxyClass(classLoader, activityManagerCls);
        final Object activityTaskManager = getActivityManager();
        Object activityManagerInstance = Proxy.newProxyInstance(classLoader, new Class[]{activityManagerCls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                Log.d(TAG, "invoke: " + name);
                if ("startActivity".equals(name)) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Intent) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.rwz.host", "com.rwz.host.PlaceholderActivity"));
                            intent.putExtra(KEY_TARGET_INTENT, (Intent)args[i]);
                            args[i] = intent;
                            break;
                        }
                    }
                }
                return method.invoke(activityTaskManager, args);
            }
        });
        Object singleton = ReflectUtil.getValue(activityManagerCls, activityTaskManager, "IActivityManagerSingleton");
        Field mInstanceField = ReflectUtil.getField(singleton.getClass(), "mInstance");
        mInstanceField.set(singleton, activityManagerInstance);
    }

    private static Object getActivityManager() throws Exception{
        Class<?> name = Class.forName("android.app.ActivityManager");
        Field field = name.getDeclaredField("getService");
        field.setAccessible(true);
        return field.get(null);
    }

    public static void hookIntentAfter(Context context) throws Exception{
        Class activityThreadCls = Class.forName("android.app.ActivityThread");
        Object activityThread = ReflectUtil.getValue(activityThreadCls, null, "currentActivityThread");
        Object mH = ReflectUtil.getValue(activityThread, "mH");
        ReflectUtil.setValue(mH, "mCallback", new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 100) {
                    try {
                        Object intent = ReflectUtil.getValue(msg.obj, "intent");
                        if (intent instanceof Intent) {
                            Intent targetIntent = ((Intent) intent).getParcelableExtra(KEY_TARGET_INTENT);
                            ReflectUtil.setValue(msg.obj, "intent", targetIntent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //不用返回true，继续由handler处理消息
                return false;
            }
        });

    }


}
