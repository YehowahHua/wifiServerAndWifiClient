package com.mycom.wifiserver.manager;

import android.support.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2018/1/30.
 */

public class ThreadPoolManager {
    private static final String TAG = "ThreadPoolManager";
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors()*2; //核心线程数为CPU*2
    private static final int MAXIMUM_POOL_SIZE = 64; //线程队列最大线程数
    private static final int KEEP_ALIVE_TIME = 1; //保持存活时间为1s

    private final BlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<>(128);


    private final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory(){
        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r,TAG + " #" + mCount.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    };

    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,mWorkQueue,DEFAULT_THREAD_FACTORY,
            new ThreadPoolExecutor.DiscardOldestPolicy());


    private static ThreadPoolManager mInstance = new ThreadPoolManager();
    public static ThreadPoolManager getInstance(){
        return mInstance;
    }

    public void addTask(Runnable runnable){
        mExecutor.execute(runnable);
    }

    public void shutdownNow(){
        mExecutor.shutdownNow();
    }


}
