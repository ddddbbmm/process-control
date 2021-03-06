package io.ddbm.pc.exception;

import lombok.Getter;

/**
 * 流程停止异常。
 */
public class InterruptException extends RuntimeException {
    @Getter
    String node;

    public InterruptException(String msg, String node) {
        super(msg);
        this.node = node;
    }

    public InterruptException(String node, Exception e) {
        super(e);
        this.node = node;
    }


    public static InterruptException noSuchEvent(String event, String node) {
        return new InterruptException(node + " not support event:" + event, node);
    }

    public static InterruptException noSuchNode(String node) {
        return new InterruptException("no such node:" + node, node);
    }

    public static InterruptException nodeIsEnd(String node) {
        return new InterruptException(node + " is end ", node);
    }
}
