package org.alien4cloud.plugin.kafka.listener.actions;

import org.alien4cloud.plugin.kafka.listener.model.Action;

public interface IAction {

   void process (Action action);

}
