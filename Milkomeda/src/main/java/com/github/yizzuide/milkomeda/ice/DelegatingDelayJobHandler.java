package com.github.yizzuide.milkomeda.ice;

import com.github.yizzuide.milkomeda.moon.Moon;
import com.github.yizzuide.milkomeda.moon.PeriodicMoonStrategy;
import com.github.yizzuide.milkomeda.universe.context.ApplicationContextHolder;
import com.github.yizzuide.milkomeda.universe.context.WebContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * DelegatingDelayJobHandler
 * 代理对多个延迟桶的调度处理
 *
 * @author yizzuide
 * @since 3.7.2
 * Create at 2020/06/11 11:24
 */
public class DelegatingDelayJobHandler implements Runnable, InitializingBean {

    @Autowired
    private JobPool jobPool;

    @Autowired
    private DelayBucket delayBucket;

    @Autowired
    private ReadyQueue readyQueue;

    @Autowired
    private DeadQueue deadQueue;

    @Autowired
    private IceProperties props;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 使用Moon来轮询延迟桶
    private Moon<DelayJobHandler> iceDelayBucketMoon;

    @Override
    public void run() {
        DelayJobHandler delayJobHandler = Moon.getPhase("ice-delay-bucket", iceDelayBucketMoon);
        delayJobHandler.run();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet() throws Exception {
        // 延迟桶处理器
        List<DelayJobHandler> delayJobHandlers = new ArrayList<>();
        for (int i = 0; i < props.getDelayBucketCount(); i++) {
            // 注册为bean，让其可以接收Spring事件
            DelayJobHandler delayJobHandler = WebContext.registerBean((ConfigurableApplicationContext) ApplicationContextHolder.get(), "delayJobHandler" + i, DelayJobHandler.class);
            delayJobHandler.fill(redisTemplate, jobPool, delayBucket, readyQueue, deadQueue, i, props);
            delayJobHandlers.add(delayJobHandler);
        }
        Moon<DelayJobHandler> moon = WebContext.registerBean((ConfigurableApplicationContext) ApplicationContextHolder.get(), "iceDelayBucketMoon", Moon.class);
        // 使用lua方式
        moon.setMixinMode(false);
        PeriodicMoonStrategy strategy = new PeriodicMoonStrategy();
        String luaScript = strategy.loadLuaScript();
        strategy.setLuaScript(luaScript);
        moon.setMoonStrategy(strategy);
        moon.add(delayJobHandlers.toArray(new DelayJobHandler[0]));
        iceDelayBucketMoon = moon;
    }
}
