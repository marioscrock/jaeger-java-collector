package eps.listener;

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.google.gson.Gson;

import eventsocket.Event;

/**
 * Simple UpdateListener implementation logging incoming {@link com.espertech.esper.client.EventBean EventBean} objects together 
 * with a {@code String} message.
 * @author Mario
 */
public class CEPListenerHL implements UpdateListener {
	
	private final static Logger log = LoggerFactory.getLogger(CEPListenerHL.class);
	private static String HL_ADDRESS = "kaiju-hl";
	
	/**
	 * Update method logging incoming {@link com.espertech.esper.client.EventBean EventBean} objects {@code newData} together 
	 * with a {@code String} message associated to the {@link CEPListenerHL CEPListener} instance.
	 */
	@Override
	public void update(EventBean[] newData, EventBean[] oldData) {
		
		if (newData != null) {
			try {
				Gson gson = new Gson();
				String jsonInString = null;
					
				if (newData.length == 1) {
					Event event = (Event) newData[0].getUnderlying();
					jsonInString = gson.toJson(event);
				} else {				
					List<Event> events = new ArrayList<>();
					for (EventBean e : newData) {			    	
				    	Event event = (Event) e.getUnderlying();
				    	events.add(event);
					}
					jsonInString = gson.toJson(((Event[])events.toArray()));
					jsonInString = "{\"events\":" + jsonInString + "}";					
				}
				
				if (System.getenv("HL_ADDRESS") != null)
					HL_ADDRESS = System.getenv("HL_ADDRESS");
				
				Socket s = new Socket(HL_ADDRESS, 11794);
				try (OutputStreamWriter out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)) {
				    out.write(jsonInString);
				}
				s.close();
				
			} catch (Exception e) {
				log.error("Error while forwarding events");
			}
		}
	}
		
}
