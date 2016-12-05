package com.mulesoft.consulting.logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Category;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.RefOnly;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.LoggerMessageProcessor;

import com.mulesoft.consulting.logger.LoggerUtils.LogType;
import com.mulesoft.consulting.logger.LoggerUtils.PayloadType;

@Connector(name = "mcs-logger-module", friendlyName = "MCS Logger Module", schemaVersion = "1.1")
@Category(name = "org.mule.tooling.ui.modules.core.miscellaneous", description = "Miscellaneous")
public class LoggerModule {

	private static String HOSTNAME;
	protected transient Log logger;

	static {
		// this is not necessarily perfect, but should be good enough. see
		// http://stackoverflow.com/a/7800008 for details
		if (System.getenv("COMPUTERNAME") != null) {
			// Windows environment variable
			HOSTNAME = System.getenv("COMPUTERNAME");
		} else if (System.getenv("HOSTNAME") != null) {
			// some Linux systems (and maybe UNIX systems) have this
			HOSTNAME = System.getenv("HOSTNAME");
		} else {
			// use the name of "localhost"
			try {
				HOSTNAME = Inet4Address.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				HOSTNAME = "[unknown]";
			}
		}
	}

	/**
	 * The Mule context is injected to access metadata such as the flow name.
	 */
	@Inject
	private MuleContext muleContext;

	public void setMuleContext(MuleContext muleContext) {
		this.muleContext = muleContext;
	}

	@Start
	public void initializeConfiguration() {
		initLogger();
	}

	protected void initLogger() {
		this.logger = LogFactory.getLog(LoggerModule.class);
	}

	@Processor(friendlyName = "Logger")
	@Category(name = "Logger", description = "MCS Logger Module Configuration")
	public MuleEvent logger(final MuleEvent event, MuleMessage muleMessage,

			@Placement(order = 1, group = "Log Settings", tab = "General") @Default("#[message.rootId]") final String correlationId,
			@Placement(order = 2, group = "Log Settings", tab = "General") @Optional @FriendlyName("Log Message") final String message,
			@Placement(order = 3, group = "Log Settings", tab = "General") @Default("INFO") @FriendlyName("Log Level") LoggerMessageProcessor.LogLevel level,
			@Placement(order = 4, group = "Log Settings", tab = "General") @Default("TEXT") PayloadType payloadType,
			@Placement(order = 4, group = "Log Settings", tab = "General") @Default("INTERMEDIATE") @FriendlyName("Log Type") LogType type,
			@Placement(order = 5, group = "Payload Options", tab = "General") @Default("false") @FriendlyName("Log Payload?") boolean logPayload,
			@Placement(order = 6, group = "Payload Options", tab = "General") @Default("false") @FriendlyName("Truncate Payload (1000 characters)?") boolean truncatePayload)

			throws InitialisationException, IOException, NullPointerException, MuleException {

		if (event == null) {
			throw new NullPointerException("The parameter 'MuleEvent' cannot be null.");
		}

		if (level == null) {
			level = LoggerMessageProcessor.LogLevel.INFO;
		}

		String strMessage = message;

		if (logPayload) {

			Object payloadObj = event.getMessage().getPayload();
			String payload = null;
			
			try {
				
				//TODO for now don't log InputStream as this closes the stream 
				// - need to look at alternatives when time permits
				if (!(payloadObj instanceof InputStream)) {
				
					if (payloadType == PayloadType.TEXT) {
						payload = LoggerUtils.formatTextPayload(event.getMessage());
					} else if (payloadType == PayloadType.JSON) {
						payload = LoggerUtils.formatJSONPayload(event.getMessage());
					} else if (payloadType == PayloadType.XML) {
						payload = LoggerUtils.formatXMLPayload(event.getMessage());
					} else {
						payload = event.getMessage().getPayloadForLogging();
					}
					
					if (truncatePayload) {
						payload = StringUtils.abbreviate(payload.toString(), 1000);
					}
				} else {
					payload = "<<<Streaming payload will not be logged>>>";
				}
					
				
			} catch (Exception e) {
				// Should never get here
				payload = String.format("<<Unable to convert payload with error '%s'>>\n%s", 
						e.getMessage(), event.getMessage().getPayloadForLogging());
			}

			strMessage = String.format("%s\nPayload: %s", message, payload);
		}

		String logMessage = "Host: " + HOSTNAME + ", LogType: " + type + ", CorrelationID:" + correlationId
				+ ", Message:" + strMessage;

		log(event, logMessage, level);

		return event;
	}

	protected void log(MuleEvent event, String message, LoggerMessageProcessor.LogLevel logLevel) {
		if (event == null) {
			logWithLevel(null, logLevel);

		} else if (StringUtils.isEmpty(message)) {
			logWithLevel(event.getMessage(), logLevel);
		} else {
			if (!(logLevel.isEnabled(this.logger)))
				return;
			logLevel.log(this.logger, this.muleContext.getExpressionManager().parse(message, event));
		}
	}

	protected void logWithLevel(Object object, LoggerMessageProcessor.LogLevel logLevel) {
		if (!(logLevel.isEnabled(this.logger)))
			return;
		logLevel.log(this.logger, object);
	}
	
}
