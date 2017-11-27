package com.zhiyuan.rxjavademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private int maxRetryCount=3;
    int currentRetryCount;
    private int waitRetryTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Retrofit retrofit=new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://fy.iciba.com/") // 设置 网络请求 Url
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        GetRequest_Interface request = retrofit.create(GetRequest_Interface.class);
        Observable<Translation> observable = request.getCall();
        observable.retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
            @Override
            public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
              return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                  @Override
                  public ObservableSource<?> apply(Throwable throwable) throws Exception {
                      // 输出异常信息
                      Log.d("",  "发生异常 = "+ throwable.toString());

                      /**
                       * 需求1：根据异常类型选择是否重试
                       * 即，当发生的异常 = 网络异常 = IO异常 才选择重试
                       */
                     if(throwable instanceof IOException){
                         Log.d(TAG,  "属于IO异常，需重试" );

                         if(currentRetryCount<maxRetryCount){
                            currentRetryCount++;
                             Log.d(TAG,  "重试次数 = " + currentRetryCount);
                             /**
                              * 需求2：实现重试
                              * 通过返回的Observable发送的事件 = Next事件，从而使得retryWhen（）重订阅，最终实现重试功能
                              *
                              * 需求3：延迟1段时间再重试
                              * 采用delay操作符 = 延迟一段时间发送，以实现重试间隔设置
                              *
                              * 需求4：遇到的异常越多，时间越长
                              * 在delay操作符的等待时间内设置 = 每重试1次，增多延迟重试时间1s
                              */
                             // 设置等待时间
                             waitRetryTime = 1000 + currentRetryCount* 1000;
                             Log.d(TAG,  "等待时间 =" + waitRetryTime);
                             return Observable.just(1).delay(waitRetryTime, TimeUnit.MILLISECONDS);


                         }else{
                             return Observable.error(new Throwable("重试次数已超过设置次数 = " +currentRetryCount  + "，即 不再重试"));
                         }
                     }
                     else{
                         return Observable.error(new Throwable("发生了非网络异常（非I/O异常）"));
                     }
                  }
              });
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                new Observer<Translation>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // 接收服务器返回的数据

                    }

                    @Override
                    public void onNext(Translation result) {
                        Log.d(TAG,  "发送成功");
                        result.show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG,  e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                }
        );

    }
}
