package jcstress;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.concurrent.atomic.AtomicInteger;

@JCStressTest
@Outcome(id = {"1, 5", "2, 5", "2, 2", "1, 2"},
        expect = Expect.ACCEPTABLE, desc = "Legal interleavings")
@State
public class JMMTest {
    private AtomicInteger ai = new AtomicInteger(5);
    private int i = 1;

    @Actor
    public void thread1(){
        i++;
        ai.set(i);
    }

    @Actor
    public void thread2(II_Result result){
        int _ai = ai.get();
        int _i = i;
        result.r1 = _i;
        result.r2 = _ai;
    }
}
