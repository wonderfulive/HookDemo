package com.phoenix.hookdemo.hook;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by lcf on 2017/5/10.
 */

public class HookUtil {
    private final static String TAG = "HookUtil";
    private Class<?> proxyActivityCls;
    private Context mContext;

    public HookUtil(Context context, Class<?> proxyActivityCls) {
        this.mContext = context;
        this.proxyActivityCls = proxyActivityCls;
    }

    /**
     * hook ActivityManager
     */
    public void hookAM() {
        try {
            //反射ActivityManagerNative
            Class<?> amnCls = Class.forName("android.app.ActivityManagerNative");
            Field singletonFiled = amnCls.getDeclaredField("gDefault");
            singletonFiled.setAccessible(true);
            //gDefault变量值(4.x以上的geDefault是一个Singleton对象，2.x ~ 3.x 是IActivityManager 对象)
            Object singletonObj = singletonFiled.get(null);
            //4.x以上的gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
            //反射Singleton,获取Singleton的mInstance 成员变量的值
            Class<?> singletonCls = Class.forName("android.util.Singleton");
            Field instanceField = singletonCls.getDeclaredField("mInstance");
            instanceField.setAccessible(true);
            //该对象即为IActivityManager的唯一对象
            Object iActivityManagerObj = instanceField.get(singletonObj);
            //要hook的接口
            Class<?> iActivityManagerCls = Class.forName("android.app.IActivityManager");
            //使用动态代理进行hook
            AMInVocationHandler inVocationHandler = new AMInVocationHandler(iActivityManagerObj);

            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{iActivityManagerCls}, inVocationHandler);

            //替换原来的IActivityManager
            instanceField.set(singletonObj, proxy);
        } catch (Exception e) {
            Log.e(TAG,"hookAM");
            e.printStackTrace();
        }
    }

    private class AMInVocationHandler implements InvocationHandler {
        private Object iActivityManagerObj;

        AMInVocationHandler(Object iActivityManagerObj) {
            this.iActivityManagerObj = iActivityManagerObj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().contains("startActivity")) {
                Intent intent = null;
                int index = -1;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        //找到startActivity方法的Intent参数
                        intent = (Intent) args[i];//原intent,实际不能通过系统检查
                        index = i;
                        break;
                    }
                }
                //创建代理intent
                Intent proxyIntent = new Intent();
                ComponentName componentName = new ComponentName(mContext, proxyActivityCls);
                proxyIntent.setComponent(componentName);
                //proxyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                proxyIntent.putExtra("oldIntent", intent);
                //使用代理intent替换原intent
                args[index] = proxyIntent;
            }
            return method.invoke(iActivityManagerObj, args);
        }
    }

    /**
     * hook ActivityThread
     */
    public void hookActivityThread() {
        try {
            Class<?> activityThreadCls = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = activityThreadCls.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            Object currentActivityThreadObj = currentActivityThreadField.get(null);

            //mH
            Field handlerField = activityThreadCls.getDeclaredField("mH");
            handlerField.setAccessible(true);
            Object handlerObj = handlerField.get(currentActivityThreadObj);

            Field launchActivityConstantField = handlerObj.getClass().getDeclaredField("LAUNCH_ACTIVITY");
            launchActivityConstantField.setAccessible(true);
            Object obj = launchActivityConstantField.get(null);
            Log.d(TAG,"LAUNCH_ACTIVITY="+obj);

            Field callbackFiled = Handler.class.getDeclaredField("mCallback");
            callbackFiled.setAccessible(true);
            //再次之前应该先获取callback的对象，判断是否为空，若不为空需要特殊处理
            callbackFiled.set(handlerObj, new ActivityThreadHandlerCallback((Handler) handlerObj));
        } catch (Exception e) {
            Log.e(TAG,"hookActivityThread");
            e.printStackTrace();
        }
    }

    private class ActivityThreadHandlerCallback implements Handler.Callback {
        private Handler handler;

        public ActivityThreadHandlerCallback(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 100) {
                Log.d(TAG,"launchActivity");
                handleLaunchActivity(msg);
            }
            handler.handleMessage(msg);
            return false;
        }

        private void handleLaunchActivity(Message msg) {
            Object activityClientRecordObj = msg.obj;
            try {
                Field intentField = activityClientRecordObj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                Intent proxyIntent = (Intent) intentField.get(activityClientRecordObj);//proxy intent
                Intent realIntent = proxyIntent.getParcelableExtra("oldIntent");
                if (realIntent != null) {
                    //使用realIntent 的 ComponentName替换 proxyIntent 的 ComponentName
                    proxyIntent.setComponent(realIntent.getComponent());
                    //使用realIntent 替换 proxyIntent
                    //intentField.set(activityClientRecordObj, realIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
