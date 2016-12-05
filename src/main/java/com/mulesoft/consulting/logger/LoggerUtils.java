package com.mulesoft.consulting.logger;

import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.mule.api.MuleMessage;

public class LoggerUtils {

	/**
	 * 
	 * @param msg
	 * @return
	 * @throws Exception
	 */
	public static String formatXMLPayload(MuleMessage msg) throws Exception {
		Document doc = msg.getPayload(Document.class);
		OutputFormat compactFormat = OutputFormat.createCompactFormat();
		compactFormat.setNewLineAfterDeclaration(false);
		StringWriter stringWriter = new StringWriter();
		XMLWriter xmlWriter = new XMLWriter(stringWriter, compactFormat);
		xmlWriter.write(doc);
		return stringWriter.toString();
	}

	public static String formatJSONPayload(MuleMessage msg) throws Exception {

		final JsonFactory factory = new JsonFactory();
		Object payload = msg.getPayload();

		JsonParser parser = null;
		StringWriter writer = null;

		if (payload instanceof byte[]) {
			byte[] bytePayload = (byte[]) payload;
			parser = factory.createJsonParser(bytePayload);
			writer = new StringWriter(bytePayload.length);
		} else if (payload instanceof String) {
			String stringPayload = (String) payload;
			parser = factory.createJsonParser(stringPayload);
			writer = new StringWriter(stringPayload.length());
		} else {
			String stringPayload = msg.getPayloadAsString();
			parser = factory.createJsonParser(stringPayload);
			writer = new StringWriter(stringPayload.length());
		}

		JsonGenerator gen = null;
		try {
			gen = factory.createJsonGenerator(writer);
			while (parser.nextToken() != null) {
				gen.copyCurrentEvent(parser);
			}
		} finally {
			if (gen != null)
				gen.close();
		}

		return writer.toString();
	}

	public static String formatTextPayload(MuleMessage msg) throws Exception {
		return msg.getPayloadForLogging("UTF-8");
	}
	
	public static enum LogType {

		/**
		 * Used for incoming payloads for a flow, usually as first element in
		 * the flow or after initial de-serialization.
		 */
		ENTRY,

		/**
		 * Used for additional data coming in from auxiliary systems.
		 */
		AUXILIARY,

		/**
		 * Used to track intermediate payloads as part of an ongoing flow.
		 */
		INTERMEDIATE,

		/**
		 * Used to track Audit type messages.
		 */
		AUDIT,

		/**
		 * Used to log a payload prior to exiting a flow.
		 */
		EXIT;

	}
	
	public static enum PayloadType {
		TEXT, JSON, XML
	}

}
