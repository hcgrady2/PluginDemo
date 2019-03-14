### Hook    

#### 一、 Hook 介绍(需要熟悉一下反射与动态代理)  
通过动态代理，我们可以自己生成代理对象，有了代理对象我们就可以调用代理对象的属性和方法，同时还能修改属性和方法的参数，替换返回值等，这就是 Hook。  
如，我们可以 Hook startActivity 这个方法，在每次跳转之前都输出一条内容（同样可以修改为跳转到自己想要的 Activity  去）    
 
首先我们得找到被 Hook 的对象，我称之为 Hook 点；什么样的对象比较好 Hook 呢？自然是容易找到的对象。什么样的对象容易找到？静态变量和单例。在一个进程之内，静态变量和单例变量是相对不容易发生变化的，因此非常容易定位，而普通的对象则要么无法标志，要么容易改变。我们根据这个原则找到所谓的 Hook 点。

对于 startActivity 过程有两种方式：Context.startActivity 和 Activity.startActivity。这里暂不分析其中的区别，以 Activity.startActivity 为例说明整个过程的调用栈。

Activity 中的 startActivity 最终都是由 startActivityForResult 来实现的。  

```
public void startActivityForResult(@RequiresPermission Intent intent, int requestCode, @Nullable Bundle options) {
  // 一般的 Activity 其 mParent 为 null，mParent 常用在 ActivityGroup 中，ActivityGroup 已废弃
  if (mParent == null) {
      options = transferSpringboardActivityOptions(options);
      // 这里会启动新的Activity，核心功能都在 mMainThread.getApplicationThread() 中完成
      Instrumentation.ActivityResult ar =
          mInstrumentation.execStartActivity(
              this, mMainThread.getApplicationThread(), mToken, this,
              intent, requestCode, options);
      if (ar != null) {
          mMainThread.sendActivityResult(
              mToken, mEmbeddedID, requestCode, ar.getResultCode(),
              ar.getResultData());
      }
      if (requestCode >= 0) {
          mStartedActivity = true;
      }
      cancelInputsAndStartExitTransition(options);
  } else {
      if (options != null) {
          mParent.startActivityFromChild(this, intent, requestCode, options);
      } else {
          // Note we want to go through this method for compatibility with
          // existing applications that may have overridden it.
          mParent.startActivityFromChild(this, intent, requestCode);
      }
  }
}

```   
可以发现，真正打开 activity 的实现在 Instrumentation 的 execStartActivity 方法中。  
```

public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode, Bundle options) {
    // 核心功能在这个whoThread中完成，其内部scheduleLaunchActivity方法用于完成activity的打开
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    Uri referrer = target != null ? target.onProvideReferrer() : null;
    if (referrer != null) {
        intent.putExtra(Intent.EXTRA_REFERRER, referrer);
    }
    if (mActivityMonitors != null) {
        synchronized (mSync) {
            final int N = mActivityMonitors.size();
            for (int i=0; i<N; i++) {
                final ActivityMonitor am = mActivityMonitors.get(i);
                ActivityResult result = null;
                if (am.ignoreMatchingSpecificIntents()) {
                    result = am.onStartActivity(intent);
                }
                if (result != null) {
                    am.mHits++;
                    return result;
                } else if (am.match(who, null, intent)) {
                    am.mHits++;
                    if (am.isBlocking()) {
                        return requestCode >= 0 ? am.getResult() : null;
                    }
                    break;
                }
            }
        }
    }
    try {
        intent.migrateExtraStreamToClipData();
        intent.prepareToLeaveProcess(who);
        // 这里才是真正打开 Activity 的地方，核心功能在 whoThread 中完成。
        int result = ActivityManager.getService()
            .startActivity(whoThread, who.getBasePackageName(), intent,
                    intent.resolveTypeIfNeeded(who.getContentResolver()),
                    token, target != null ? target.mEmbeddedID : null,
                    requestCode, 0, null, options);
        // 这个方法是专门抛异常的，它会对结果进行检查，如果无法打开activity，
            // 则抛出诸如ActivityNotFoundException类似的各种异常
        checkStartActivityResult(result, intent);
    } catch (RemoteException e) {
        throw new RuntimeException("Failure from system", e);
    }
    return null;
}

```  

我们的目的是替换掉系统默认逻辑，对于 Activity#startActivityForResult 的方法里面核心逻辑就是 mInstrumentation 属性的 execStartActivity 方法，而这里的 mInstrumentation 属性在 Activity 类中恰好是一个单例，在 Activity 类的 attach 方法里面被赋值，我们可以在 attach 之后使用反射机制对 mInstrumentation 属性进行重新赋值。attach() 方法调用完成后，就自然而然的调用了 Activity 的 onCreate() 方法了。  

我们需要修改 mInstrumentation 这个字段为我们的代理对象，我们使用静态代理实现这个代理对象。这里我们使用 EvilInstrumentation 作为代理对象。 

```
public class EvilInstrumentation extends Instrumentation {
    private static final String TAG = "EvilInstrumentation";
    private Instrumentation instrumentation;

    public EvilInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        StringBuilder sb = new StringBuilder();
        sb.append("who = [").append(who).append("], ")
                .append("contextThread = [").append(contextThread).append("], ")
                .append("token = [").append(token).append("], ")
                .append("target = [").append(target).append("], ")
                .append("intent = [").append(intent).append("], ")
                .append("requestCode = [").append(requestCode).append("], ")
                .append("options = [").append(options).append("]");;
        Log.i(TAG, "执行了startActivity, 参数如下: " + sb.toString());

        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class,
                    Bundle.class);
            return (ActivityResult) execStartActivity.invoke(instrumentation, who, contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
```  
Hook 
```
    private void test(Activity activity){

        try {
            // 拿到原始的 mInstrumentation字段
            Field mInstrumentationField  = Activity.class.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation originalInstrumentation = (Instrumentation) mInstrumentationField.get(activity);
            mInstrumentationField.set(activity, new EvilInstrumentation(originalInstrumentation));


        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


    }

```

调用：  

```
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        launch = findViewById(R.id.luanch);
        test = findViewById(R.id.test);

        test(MainActivity.this);

        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPluginManager.loadPlugin(Constants.PLUGIN_PATH)) {
                    Intent intent = new Intent();
                    intent.setClassName(mPluginPackageName, mPluginClassName);
					//这里在调用，就生效了
                    startActivity(intent);
                }
            }
        });

    }

```   



#### 二、Resouce Hook
插件化首先需要解决的是 Resouce Hook 问题。  
application,activity,service 和 Broadcast 通过 getResouce 方法都会走到 ContextImpl 中。  
```
  public class ContextWrapper extends Context {
    Context mBase; //mBase是ContextImpl实例

    public ContextWrapper(Context base) {
        mBase = base;
    }
    @Override
    public Resources getResources()
    {
        return mBase.getResources();
    }
 }
```

因此需要理解两个问题：  
+ Resouce 是如何注入 ContextImpl 中的
+ Application,Activity,Service和Broadcast是在什么时机注入ContextImpl实例的（进而获取 Resouce）  

##### 1、Application 与 ContextImpl   

ContextWrapper.java   
```
  //ContextWrapper的attachBaseContext方法关联了mBase，这里的mBase就是ContextImpl实例，我们往下看
    protected void attachBaseContext(Context base) {
        if (mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBase = base;
    }
```

application.java    
``` 
 final void attach(Context context) {
      attachBaseContext(context);
      mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
   }
```

Instrumentation.java  
```
public class Instrumentation {

    //LoadedApk.makeApplication会调用
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {   
        return newApplication(cl.loadClass(className), context);
    }
    
   //Application在这里被创建
   static public Application newApplication(Class<?> clazz, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {
        Application app = (Application)clazz.newInstance();
        app.attach(context);
        return app;
    }
}  
```  

LoadedApk.java  
```
public final class LoadedApk {
  public Application makeApplication(boolean forceDefaultAppClass,Instrumentation instrumentation) {
         // ......
         //这里终于看到ContextImpl被创建了，并通过Instrumentation.newApplication与Application关联起来了
         //另外这里createAppContext是ContextImpl的mResource与LoadApk的mResource关联的核心代码                              
        ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this);
        app = mActivityThread.mInstrumentation.newApplication(
                cl, appClass, appContext);
        // ......
        mApplication = app;
        // ......
        return app;
  }
}
```  

总结如下：  
+ Luancher APP处理点击，会调用到AMS。ActivityManagerService发送BIND_APPLICATION消息到ActivityThread，ActivityThread.handleBindApplication中调用了LoadedApk.makeApplication方法   
+ ActivityThread.makeApplication方法创建了ContextImpl实例，并作为参数调用Instrumentation.newApplication方法  
+ Instrumentation.newApplication方法完成Application实例创建，并在application.attach方法完成Application实例与ContextImpl的关联


#### 2、CotextImpl 与 Resouce  

由上面分析：
ContextImpl.createAppContext方法是ContextImpl实例的mResource与LoadApk实例的mResource关联的核心代码，接下来我们看下createAppContext方法  
```
class ContextImpl extends Context {
  static ContextImpl createAppContext(ActivityThread mainThread, LoadedApk packageInfo) {
          if (packageInfo == null) throw new IllegalArgumentException("packageInfo");
           return new ContextImpl(null, mainThread,
                packageInfo, null, null, 0, null, null, Display.INVALID_DISPLAY);
       }
  private ContextImpl(ContextImpl container, ActivityThread mainThread,
                            LoadedApk packageInfo, IBinder activityToken, UserHandle user, int flags,
                            Display display, Configuration overrideConfiguration, int createDisplayWithId) {
            //...
            //从LoadApk创建Resources 实例
            Resources resources = packageInfo.getResources(mainThread);
            //...
            mResources = resources;
            //...
        }
}
```  

LoadedApk.java  
```
//LoadedApk类
public Resources getResources(ActivityThread mainThread) {
        if (mResources == null) {
            mResources = mainThread.getTopLevelResources(mResDir, mSplitResDirs, mOverlayDirs,
                    mApplicationInfo.sharedLibraryFiles, Display.DEFAULT_DISPLAY, this);
        }
        return mResources;
}
```

也就是说，ContextImpl 的 Resouce 是从 LoadedApk 的 getResouce 获取的，这里我们可以进行 Hook 操作。


#### 3、插件与宿主 APK 资源合并  
 
先看代码：
```
public class Resources {
  public CharSequence getText(@StringRes int id) throws NotFoundException {
        CharSequence res = mAssets.getResourceText(id);
        //......
    }
  public String[] getStringArray(@ArrayRes int id)
            throws NotFoundException {
        String[] res = mAssets.getResourceStringArray(id);
        //......
    }
}    
```  

与getText，getStringArray等方法获取资源类似，都会调用mAssets。getResourcexxx方法，mAssets是一个AssetManager对象是从Resource构造函数中赋值。  
```
 /**
     * Create a new Resources object on top of an existing set of assets in an
     * AssetManager.
     *
     * @param assets Previously created AssetManager.
     * @param metrics Current display metrics to consider when
     *                selecting/computing resource values.
     * @param config Desired device configuration to consider when
     *               selecting/computing resource values (optional).
     */
    public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        this(assets, metrics, config, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO);
    }

    /**
     * Creates a new Resources object with CompatibilityInfo.
     *
     * @param assets Previously created AssetManager.
     * @param metrics Current display metrics to consider when
     *                selecting/computing resource values.
     * @param config Desired device configuration to consider when
     *               selecting/computing resource values (optional).
     * @param compatInfo this resource's compatibility info. Must not be null.
     * @hide
     */
    public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config,
            CompatibilityInfo compatInfo) {
        mAssets = assets;
        mMetrics.setToDefaults();
        if (compatInfo != null) {
            mCompatibilityInfo = compatInfo;
        }
        updateConfiguration(config, metrics);
        assets.ensureStringBlocks();
    }
```   

我们先忽略除assets入参以外的参数，AssetManager有一个关键方法 addAssetPath，可以把额外的apk或目录的资源加入到AssetManager实例中。并且额外的一个关键点，AssetManager是一个单例。  

```
 /**
     * Add an additional set of assets to the asset manager.  This can be
     * either a directory or ZIP file.  Not for use by applications.  Returns
     * the cookie of the added asset, or 0 on failure.
     * {@hide}
     */
    public final int addAssetPath(String path) {
        synchronized (this) {
            int res = addAssetPathNative(path);
            makeStringBlocks(mStringBlocks);
            return res;
        }
    }
  
```   

也就是说，如果我们把AssetManager单例加入插件的资源或宿主的资源，那资源共享就解决了一大半。 
资源共享另一半问题是我们要解决资源id突冲问题，这篇我们不细说，解决方案目前有重写aapt,arsc等方案。  

#### 4、Activity 与 ContextImpl  

前面我们看到ContextWrapper是在attachBaseContext中关联ContextImpl对象的。先看下Activity.attachBaseContext在什么方法中调用。  
```
//Activity.attach方法
    final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor) {
        attachBaseContext(context);
        //.....
    }
```  
 从代码看到，Activity.attach方法执行了attachBaseContext。Instrumentation管理Activity创建和生命周期回调。下面看下Instrumentation.performLaunchActivity方法。
 ```
  private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
         //......   
        Activity activity = null;
        //......   
            activity = mInstrumentation.newActivity(
                    cl, component.getClassName(), r.intent);
        //......
                //createBaseContextForActivity返回了ContextImpl实例    
                Context appContext = createBaseContextForActivity(r, activity);
        //......    
                activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor);

        //......    
        return activity;
    }
 ```
 
 也就是说，context 由 createBaseContextForActivity 获取。  
 ```
    private Context createBaseContextForActivity(ActivityClientRecord r, final Activity activity) {
        //.....
        //ContextImpl.createActivityContext返回了ContextImpl实例   
        ContextImpl appContext = ContextImpl.createActivityContext(
                this, r.packageInfo, displayId, r.overrideConfig);
        appContext.setOuterContext(activity);
        Context baseContext = appContext;
        //.....
        return baseContext;
    }
 ```  
 
 转至ContextImpl.createActivityContext方法  
 
```
 static ContextImpl createActivityContext(ActivityThread mainThread,
            LoadedApk packageInfo, int displayId, Configuration overrideConfiguration) {
        if (packageInfo == null) throw new IllegalArgumentException("packageInfo");
        return new ContextImpl(null, mainThread, packageInfo, null, null, false,
                null, overrideConfiguration, displayId);
    }
```  

上面我们分析到ContextImpl构造函数会将LoadApk的mResource赋值给ContextImpl的mResource。至此，我们可以确认Activity和Application一样，mBase.mResource就是LoadApk的mResource。   

#### 5、Service 与 ContextImpl  
Service与Activity类似，Service.attach在ActivityThread.handleCreateService调用。  
```
private void handleCreateService(CreateServiceData data) {
            //......
            service = (Service) cl.loadClass(data.info.name).newInstance();
            //......
            ContextImpl context = ContextImpl.createAppContext(this, packageInfo);
            context.setOuterContext(service);

            Application app = packageInfo.makeApplication(false, mInstrumentation);
            service.attach(context, this, data.info.name, data.token, app,
                    ActivityManagerNative.getDefault());
            //......
    }
```  

上面我们分析到ContextImpl.createAppContext会执行构造函数，在构造函数会将LoadedApk的mResource赋值给ContextImpl的mResource。至此，我们可以确认Service和Application一样，mBase.mResource就是LoadApk的mResource。  


#### 6 、Broadcast 与 ContextImpl  
```
 private void handleReceiver(ReceiverData data) {
      
        //......
        BroadcastReceiver receiver;
        try {
            java.lang.ClassLoader cl = packageInfo.getClassLoader();
            data.intent.setExtrasClassLoader(cl);
            data.intent.prepareToEnterProcess();
            data.setExtrasClassLoader(cl);
            receiver = (BroadcastReceiver)cl.loadClass(component).newInstance();
        } catch (Exception e) {
            //......
        }

            //......
            ContextImpl context = (ContextImpl)app.getBaseContext();
            sCurrentBroadcastIntent.set(data.intent);
            receiver.setPendingResult(data);
            
            //receiver.onReceive传入的是ContextImpl.getReceiverRestrictedContext返回对象
            receiver.onReceive(context.getReceiverRestrictedContext(),
                    data.intent);
        //......
    }
```   

```
  //ContextImpl.getReceiverRestrictedContext
    final Context getReceiverRestrictedContext() {
        if (mReceiverRestrictedContext != null) {
            return mReceiverRestrictedContext;
        }
        return mReceiverRestrictedContext = new ReceiverRestrictedContext(getOuterContext());
    }
```  

ReceiverRestrictedContext也是继承ContextWrapper，其mBase是Application。


总结：Application,Activity,Service和Broadcast均会通过LoadedApk.mResource去获取资源，我们只要HOOK LoadedApk的mResource替换我们的Resource即可。


#### Hook 插件化实现

一 、自己定义 Instrumentation 
1、这里首先 hook execStartActivity，跳转到实现定义好的 stubActivity，绕过 manifest 检测   
2、重写 newActivity，当系统回调时，跳到真正的插件类。   

二、使用自定义Instrumentation       
1、 通过反射，拿到 ActivityThread 对象，进而拿到 AdtivityThread 中的 mInstrumentation ,拿到 Activity 中的 mInstrumentation      
2、通过反射，设置 ActivityThread 与 Activity 中的 mInstrumentation 为我们自定义的 Instrumentation 对象  

三、加载 apk  
1、实现插件化跳转之前，肯定需要加载 apk 加载插件就是把插件添加到系统 addAssetPath 路径中,并且要有对应的 classLoader 才能调用

四、完成插件化
、intent 实现跳转   
```
   Intent intent = new Intent();
                    intent.setClassName(mPluginPackageName, mPluginClassName);
                    startActivity(intent);
					
```

参考：   
[1] http://weishu.me/2016/01/28/understand-plugin-framework-proxy-hook/  
[2] https://zhuanlan.zhihu.com/p/33017826   
[3] https://cloud.tencent.com/developer/article/1351074   
[4] https://segmentfault.com/a/1190000013048236











