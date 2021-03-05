package org.alien4cloud.plugin.kafka.listener.actions;

import org.alien4cloud.plugin.kafka.listener.model.Action;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAction {

   public abstract Action process (Action action);

   protected Action initResponse(Action request) {
       Action response = new Action();
       response.setAction("ack");
       response.setRequestid(request.getRequestid());
       return response;
   }

   protected Action completeResponse (Action response, String status, String message) {
       response.setDatetime((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(new Date()).toString());      
       Map<String,String> parameters = new HashMap<String,String>();
       response.setParameters(parameters);
       parameters.put ("status", status);
       if (message != null) {
          parameters.put ("message", message);
       }
       return response;
   }


}
