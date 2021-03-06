package io.ddbm.pc;

import io.ddbm.pc.exception.InterruptException;
import io.ddbm.pc.exception.PauseException;
import io.ddbm.pc.support.ActionPlugins;
import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class Event {
    Task          on;
    String        event;
    ActionPlugins action;
    Integer       retry;
    Set<String>   maybe;
    String        desc;


    public Event(Task on, String event, String actionDSL, String maybe, String desc, String retry) {
        Assert.notNull(event);
        Assert.notNull(on);
        this.on     = on;
        this.event  = event;
        this.action = new ActionPlugins(actionDSL, Action.dsl(actionDSL));
        if (StringUtils.isEmpty(maybe)) {
            HashSet<String> s = new HashSet<>();
            s.add(on.getName());
            this.maybe = s;
        } else if (!maybe.contains(",")) {
            HashSet<String> s = new HashSet<>();
            s.add(maybe);
            this.maybe = s;
        } else {
            this.maybe = Arrays.stream(maybe.split(",")).collect(Collectors.toSet());
        }
        this.desc  = desc;
        this.retry = StringUtils.isEmpty(retry) ? Coast.DEFAULT_RETRY : Integer.valueOf(retry);
    }


    public void execute(FlowContext ctx) throws PauseException, InterruptException {
        this.action.execute(ctx);
    }

    public String getActionName() {
        return action.getActionName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event command = (Event) o;
        return Objects.equals(event, command.event) && Objects.equals(on, command.on);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, on);
    }
}