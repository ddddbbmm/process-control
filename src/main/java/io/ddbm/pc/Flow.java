package io.ddbm.pc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * 流程定义
 */
public class Flow implements ValueObject {
    Logger logger = LoggerFactory.getLogger(getClass());
    //    缺省指令
    static final String DEFAULT_COMMAND = "push";
    //    流程名
    String                        name;
    //    节点名称：节点
    Map<String, _Node>            nodes;
    Map<String, ExpressionRouter> routers;
    //    开始节点
    _Node                         startNode;
    //    流程数据的持久化接口，需要用户实现
    ContextService                contextService;

    public Flow(String name) {
        Assert.notNull(name, "工作流名称为空");
        this.name    = name;
        this.nodes   = new HashMap<>();
        this.routers = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (_Node t : nodes.values()) {
            if (t instanceof Start) {
                startNode = t;
                break;
            }
        }
        Assert.notNull(startNode, "开始节点为空");
    }

    public FlowContext execute(FlowRequest request, String cmd) throws RouterException {
        Assert.notNull(request, "FlowRequest is null");
        Assert.notNull(cmd, "CMD is null");
//        获取当前数据节点
        _Node       currentNode = getNodeOfRequest(request);
//        构建上下文
        FlowContext ctx         = FlowContext.of(request);
        _Node       targetNode  = currentNode.execute(this, currentNode, ctx, cmd);
//        获取下一个节点
        ctx.postExecute(targetNode);
//        更新上下文
        contextService.snapshot(ctx);
//        判断是否重复执行
        if (ctx.isRetry()) {
            return ctx;
        }
//        继续执行下一个节点
        if (null != targetNode && !(targetNode instanceof End)) {
            execute(request, cmd);
        }
        return ctx;
    }

    public _Node getNodeOfRequest(FlowRequest request) {
        if (StringUtils.isEmpty(request.getNode())) {
            return startNode;
        } else {
            return nodes.get(request.getNode());
        }
    }

    public _Node getNode(String nodeName) {
        return nodes.get(nodeName);
    }


    public void addNode(_Node node) {
        node.setFlow(this);
        this.nodes.put(node.name, node);
    }

    public void addRouter(ExpressionRouter router) {
        this.routers.put(router.routerName, router);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flow flow = (Flow) o;
        return Objects.equals(name, flow.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Router getRouter(String routerName) {
        return routers.get(routerName);
    }
}
