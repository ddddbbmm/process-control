package io.ddbm.pc;

import io.ddbm.pc.exception.InterruptException;
import io.ddbm.pc.exception.PauseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

public class Pc implements ApplicationContextAware, ApplicationListener<Pc.FlowEvent> {
    Logger logger = LoggerFactory.getLogger(getClass());
    private ApplicationContext app;

    public FlowContext execute(String flowName, FlowRequest request, String event) throws PauseException, InterruptException {
        Assert.notNull(flowName, "flowName is null");
        Flow flow = Flows.get(flowName);
        Assert.notNull(flow, "flow[" + flowName + "] not exist");
        return flow.execute(request, event);
    }

    /**
     * 一次请求，跑到结束或者异常 ，并发挥异常
     */
    public FlowContext sync(String flowName, FlowRequest request, String event) throws InterruptException {
        FlowContext ctx = null;
        try {
            ctx = execute(flowName, request, event);
        } catch (PauseException e) {
            ctx = e.getCtx();
        } finally {
            final FlowContext _ctx = ctx;
            if (null != _ctx && !_ctx.isStop(logger)) {
                sync(flowName, request, event);
            }
        }
        return ctx;
    }

    /**
     * 一次请求，跑到结束或者异常，首节点返回。其他节点异步。
     */
    public FlowContext async(String flowName, FlowRequest request, String event) throws InterruptException {
        FlowContext ctx = null;
        try {
            ctx = execute(flowName, request, event);
        } catch (PauseException e) {
            ctx = e.getCtx();
        } finally {
            final FlowContext _ctx = ctx;
            if (null != _ctx && !_ctx.isStop(logger)) {
                app.publishEvent(new FlowEvent(flowName, request, ctx));
            }
        }
        return ctx;
    }


    @Override
    public void onApplicationEvent(FlowEvent fe) {
        try {
            async(fe.flowName, fe.request, Coast.DEFAULT_EVENT);
        } catch (InterruptException e) {
            fe.ctx.setInterrupt(true, e);
        }
    }


    public FlowContext test(String flowName, FlowRequest request, String event) throws PauseException, InterruptException {
        Assert.notNull(flowName, "flowName is null");
        Flow flow = Flows.get(flowName);
        Assert.notNull(flow, "flow[" + flowName + "] not exist");
        injectMockAction(flow);
        return execute(flowName, request, event);
    }

    private void injectMockAction(Flow flow) {
        flow.getNodes().forEach((nn, node) -> {
            node.events.forEach((en, event) -> {
                List<Action> action = Arrays.asList(new Action() {
                    //                    构建混沌action
                    @Override
                    public void execute(FlowContext ctx) throws PauseException, InterruptException {
                        Double d = Math.random();
                        if (d < 0.1) {
                            throw new PauseException(ctx, "随机出个暂停异常");
                        } else if (d < 0.2) {
//                            throw new InterruptException("随机出个中断异常", ctx.getRequest().getStatus());
                        } else if (d < 0.3) {
                            throw new RuntimeException("随机出个异常");
                        } else if (d < 0.4) {
                            ctx.getRequest().setStatus("_randomNode");
                        } else {
//                        随机出个目标节点
                            String chaoNode = ctx.chaosNode();
                            ctx.getRequest().setStatus(chaoNode);
                        }
                    }
                });
                event.action.setAction(action);
            });
        });
    }

    public FlowContext chaos(String flowName, FlowRequest request, String event) throws InterruptException {
        FlowContext ctx = null;
        try {
            try {
                ctx = test(flowName, request, event);
            } catch (PauseException e) {
                ctx = e.getCtx();
            }
        } finally {
            if (null != ctx && !ctx.isStop()) {
                try {
                    chaos(flowName, request, Coast.DEFAULT_EVENT);
                } catch (InterruptException e) {
                    ctx.setInterrupt(true, e);
//                    logger.info("flow:{},id:{} interrupt ", flowName, request.getId());
                }
            }
        }
        return ctx;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.app = applicationContext;
    }


    protected class FlowEvent extends ApplicationEvent {
        private final String      flowName;
        private final FlowRequest request;
        private final FlowContext ctx;

        public FlowEvent(String flowName, FlowRequest request, FlowContext ctx) {
            super(request);
            this.flowName = flowName;
            this.request  = request;
            this.ctx      = ctx;
        }
    }
}
