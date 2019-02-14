package eps;

import thriftgen.*;
import thriftgen.Process;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;

import eventsocket.Event;
import eventsocket.FLog;
import eventsocket.Metric;

/**
 * Class to manage the Esper Engine.
 * @author Mario
 */
public class EsperHandler {
	
	public static String RETENTION_TIME = "2min";
	public static String MODE = "logs"; //traces (or traces-api), metrics, logs or high-level
	
	protected static EPRuntime cepRT;
	protected static EPAdministrator cepAdm;
	
	/**
	 * Static method to initialize the Esper Engine and the selected statements, the Kaiju API.
	 */
	public static void initializeHandler() {
		
		//Check not already initialized
		if(cepRT == null) {
			
			//The Configuration is meant only as an initialization-time object.
		    Configuration cepConfig = new Configuration();
		    
		    /*
		     * Basic EVENTS
		     */
		    addEventTypes(cepConfig);
		 
		    // We setup the engine
		    EPServiceProvider cep = EPServiceProviderManager.getProvider("myCEPEngine", cepConfig);
		    
		    cepRT = cep.getEPRuntime();
		    cepAdm = cep.getEPAdministrator();
		    
		    // True if taken from file, false if default statements
		    initializeStatements(cepAdm, false);
		    
		    /*
		     * START API
		     */
		    if (EsperHandler.MODE.equals("traces-api")) {
		    	
			    Thread APIThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						KaijuAPI.initAPI();	
					}
					
				});
		    
		    	APIThread.run();
			}
		}
	    
	}
	
	private static void initializeStatements(EPAdministrator cepAdm, boolean fromFile) {
		
		if (fromFile) {
			EsperStatements.parseStatements(cepAdm, RETENTION_TIME);
		} else {
			switch (MODE) {
			case "traces":
			case "traces-api":
				EsperStatements.defaultStatementsTraces(cepAdm, RETENTION_TIME);
				break;
			case "metrics":
				EsperStatements.defaultStatementsMetrics(cepAdm, RETENTION_TIME);
				break;
			case "logs":
				EsperStatements.defaultStatementsLogs(cepAdm, RETENTION_TIME);
				break;
			case "high-level":
				EsperStatements.defaultStatementsHighLevel(cepAdm, RETENTION_TIME);
				break;
			default:
				break;
			}
			
		}
		
	}

	private static void addEventTypes(Configuration cepConfig) {
		
	    // We register thriftgen classes as objects the engine will have to handle
		// JAEGER model
	    cepConfig.addEventType("Batch", Batch.class.getName());
	    cepConfig.addEventType("Span", Span.class.getName());
	    cepConfig.addEventType("Process", Process.class.getName());
	    cepConfig.addEventType("Log", Log.class.getName());
	    cepConfig.addEventType("Tag", Tag.class.getName());
	    cepConfig.addEventType("SpanRef", SpanRef.class.getName());
	   
	    // SOCKET events
	    // Metrics -> JSON influxDB
	    // Events -> Custom definition
	    // Flog -> Json format from Fluentd
	    // We register metrics and events as objects the engine will have to handle
	    cepConfig.addEventType("Metric", Metric.class.getName());
	    cepConfig.addEventType("Event", Event.class.getName());
	    cepConfig.addEventType("FLog", FLog.class.getName());
		
	}

	/**
	 * Static method to send a {@link thriftgen.Batch Batch} event, and a {@link thriftgen.Span Span} for each span 
	 * in the batch to the Esper engine.
	 * @param batch The {@link thriftgen.Batch Batch} to be sent to the Esper engine.
	 */
	public static void sendBatch(Batch batch) {
		
		cepRT.sendEvent(batch);
		
		if(batch.getSpans() != null) {
			
			for(Span span : batch.getSpans()) {
				cepRT.sendEvent(span);			
			}
		}
	}
	
	/**
	 * Static method to send a {@link eventsocket.Metric Metric} event to the Esper engine.
	 * @param metric The {@link eventsocket.Metric Metric} to be sent to the Esper engine.
	 */
	public static void sendMetric(Metric metric) {
		
		cepRT.sendEvent(metric);
		
	}
	
	/**
	 * Static method to send a {@link eventsocket.Event Event} event to the Esper engine.
	 * @param event The {@link eventsocket.Event Event} to be sent to the Esper engine.
	 */
	public static void sendEvent(Event event) {

		cepRT.sendEvent(event);
		
	}
	
	/**
	 * Static method to send a {@link eventsocket.FLog FLog} event to the Esper engine.
	 * @param event The {@link eventsocket.FLog FLog} to be sent to the Esper engine.
	 */
	public static void sendFLog(FLog flog) {

		cepRT.sendEvent(flog);
		
	}

}