package com.simple.lightnote.rx;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;

/**
 * Created by homelink on 2016/8/29.
 */
public class BufferTest {
    @Test
    public void test() {
        Observable.range(1, 200)
                .interval(100, 100, TimeUnit.MILLISECONDS)
                .buffer(0, 300, TimeUnit.MILLISECONDS)
                .subscribe(System.out::println);
    }

    @Test
    public void test1() {
        Observable.interval(1000, TimeUnit.MILLISECONDS)
                .range(20, 200)
                .take(5)
                .window(250, 1, TimeUnit.MILLISECONDS)
                .flatMap(o -> o.toList())
                .subscribe(System.out::println);
    }


    @Test
    public void test2() {
        Observable<Long> just = Observable.just(System.currentTimeMillis());
        just.subscribe(System.out::println);
        System.out.println("just: ");
        just.subscribe(System.out::println);

        /**
         * 注意上面打印的两个时间是一样的。
         */

        Observable<Object> defer = Observable.defer(new Func0<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return Observable.just(System.currentTimeMillis());
            }
        });
        defer.subscribe(System.out::println);
        System.out.println("defer: ");
        defer.subscribe(System.out::println);
        /**
         * 使用defer进行封装后打印时间
         *
         * defer直到被订阅才会被执行
         *
         */
    }

}
