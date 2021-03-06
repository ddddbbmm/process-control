package io.ddbm.pc

import io.ddbm.pc.config.PcConfiguration
import org.junit.Before
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

class BaseTest {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    public Pc pc;

    @Before
    public void setup() {
        Coast.CHAOS_MODE = Boolean.TRUE;
        ctx.register(PcConfiguration)
        ctx.register(PcConfig)
        ctx.refresh()

        pc = ctx.getBean(Pc.class)
    }

    @Configuration
    @ComponentScan(basePackages = "io.ddbm.pc.simple", includeFilters = [
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Action.class)
    ])
    static class PcConfig {
    }
}
