package com.progress.codeshare.esbservice.convertMessage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQLog;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQMessageException;
import com.sonicsw.xq.XQMessageFactory;
import com.sonicsw.xq.XQParameterInfo;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.XQService;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceException;
import com.sonicsw.xq.service.sj.MessageUtils;

public class ConvertMessageServiceType implements XQService {

	// This is the XQLog (the container's logging mechanism).
	private XQLog m_xqLog = null;

	// This is the the log prefix that helps identify this service during
	// logging
	private static String m_logPrefix = "";

	// These hold version information.
	private static int s_major = 4;

	private static int s_minor = 1;

	private static int s_buildNumber = 0;

	private static final String PARAM_NAME_FILE_NAME_LAYOUT = "fileNameLayout";

	private static final String PARAM_NAME_MESSAGE_PART = "messagePart";

	/**
	 * Constructor for a ConvertMessageService
	 */
	public ConvertMessageServiceType() {
	}

	/**
	 * Initialize the XQService by processing its initialization parameters.
	 * 
	 * <p>
	 * This method implements a required XQService method.
	 * 
	 * @param initialContext
	 *            The Initial Service Context provides access to:<br>
	 *            <ul>
	 *            <li>The configuration parameters for this instance of the
	 *            PassThroughService.</li>
	 *            <li>The XQLog for this instance of the PassThroughService.</li>
	 *            </ul>
	 * @exception XQServiceException
	 *                Used in the event of some error.
	 */
	public void init(XQInitContext initialContext) throws XQServiceException {
		XQParameters params = initialContext.getParameters();
		m_xqLog = initialContext.getLog();
		setLogPrefix(params);

		m_xqLog.logInformation(m_logPrefix + " Initializing ...");

		writeStartupMessage(params);
		writeParameters(params);
		// perform initilization work.

		m_xqLog.logInformation(m_logPrefix + " Initialized ...");
	}

	private void convertMessageServiceContext(XQServiceContext ctx)
			throws XQServiceException {

		final XQParameters params = ctx.getParameters();

		final String fileNameLayout = params.getParameter(
				PARAM_NAME_FILE_NAME_LAYOUT, XQConstants.PARAM_STRING);
		
		m_xqLog.logInformation("value [" + params.getParameter(PARAM_NAME_MESSAGE_PART, XQConstants.PARAM_STRING) + "]");
		
		final int messagePart = 0; 
			//params.getIntParameter(PARAM_NAME_MESSAGE_PART, XQConstants.PARAM_STRING);

		try {

			// Get the message.
			XQEnvelope env = null;
			while (ctx.hasNextIncoming()) {
				env = ctx.getNextIncoming();
				if (env != null) {
					XQMessage msg = env.getMessage();

					for (int i = 0; i < msg.getPartCount(); i++) {

						if ((messagePart == i)
								|| (messagePart == XQConstants.ALL_PARTS)) {

							try {

								final XQPart part = msg.getPart(i);

								ArrayList<FileStructure> list;
								String thisLine = null;

								m_xqLog.logDebug("fileNameLayout: "
										+ fileNameLayout + "\n");

								list = loadProperties(fileNameLayout);

								String dbXML = null;

								InputStream is = new ByteArrayInputStream(part
										.getContent().toString().getBytes());

								BufferedReader br = new BufferedReader(
										new InputStreamReader(is));

								while ((thisLine = br.readLine()) != null) { // while
									// loop begins here

									dbXML = createDBXML(list, thisLine);
									m_xqLog.logDebug("msgContent: \n" + dbXML);

									final XQMessageFactory msgFactory = ctx
											.getMessageFactory();
									final XQMessage newMsg = msgFactory
											.createMessage();

									/*
									 * Copy all headers from the original
									 * message to the new message
									 */
									MessageUtils.copyAllHeaders(msg, newMsg);

									final XQPart newPart = newMsg.createPart();

									newPart.setContentId("0");

									newPart.setContent(dbXML,
											XQConstants.CONTENT_TYPE_XML);

									newMsg.addPart(newPart);

									env.setMessage(newMsg);

									final Iterator addressIterator = env
											.getAddresses();

									if (addressIterator.hasNext())
										ctx.addOutgoing(env);

								} // end while

							} catch (XQMessageException me) {
								throw new XQServiceException(
										"Exception accessing XQMessage: "
												+ me.getMessage(), me);
							}
						}
					}
				}
			}

		} catch (final Exception e) {
			throw new XQServiceException(e);
		}
	}

	private String createDBXML(ArrayList<FileStructure> list, String line)
			throws Exception {

		final StringBuilder builder = new StringBuilder();

		final StringBuilder builderRows = new StringBuilder();

		final String NAMESPACE_URI_DEFAULT_PROGRESS = "http://www.sonicsw.com/esb/service/dbservice";

		boolean isRows = false;

		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		builder
				.append("<db:result xmlns:db=\""
						+ NAMESPACE_URI_DEFAULT_PROGRESS
						+ "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
						+ " xsi:schemaLocation=\"http://www.sonicsw.com/esb/service/dbservice sonicfs:///System/Schemas/esb/service/DBService.xsd\">");
		builder.append("<db:resultSet version=\"1.1\">");

		builderRows.append("<db:row>");

		for (final FileStructure sFile : list) {

			m_xqLog.logDebug(" name colum: [" + sFile.getName() + "]");

			isRows = true;

			builderRows.append("<db:" + sFile.getDisplayColumn().toUpperCase()
					+ ">");
			String sReturn = line.substring(sFile.getStartPosition(), sFile
					.getStartPosition()
					+ sFile.getSize());

			if (sFile.getType().equalsIgnoreCase("decimal")) {

				Formatter formatter = new Formatter(Locale.US);
				int operator = (int) Math.pow(10, Double.valueOf(sFile
						.getPrecision()));

				Float parseFloat = Float.parseFloat(sReturn);

				parseFloat = (float) parseFloat / operator;

				m_xqLog.logDebug("operator: [" + operator + "]");
				
				m_xqLog
						.logDebug("value: [" + Double.parseDouble(sReturn)
								+ "]");
				builderRows.append(formatter.format("%(."
						+ sFile.getPrecision() + "f", parseFloat));
				
				m_xqLog.logDebug("parseDouble: ["
						+ formatter.format("%(." + sFile.getPrecision() + "f",
								parseFloat) + "]");
			} else {

				if (sFile.isRemoveTrim()) {
					builderRows.append(sReturn.trim());
				} else {
					builderRows.append(sReturn);
				}
			}

			builderRows.append("</db:" + sFile.getDisplayColumn().toUpperCase()
					+ ">");

		}

		builderRows.append("</db:row>");

		if (isRows) {
			builder.append(builderRows.toString());
		}

		builder.append("</db:resultSet>");
		builder.append("</db:result>");

		return builder.toString();
	}

	private ArrayList<FileStructure> loadProperties(String fileNameLayout)
			throws Exception {

		ArrayList<FileStructure> list = new ArrayList<FileStructure>();

		Properties defaultProps = new Properties();

		InputStream is = new ByteArrayInputStream(fileNameLayout.getBytes());

		defaultProps.load(is);

		String[] runtimeParamNames = null;
		runtimeParamNames = defaultProps.getProperty("type.runtimeParamNames")
				.split(",");

		String runtimeParam = "runtimeParam.%s.%s";

		for (int i = 0; i < runtimeParamNames.length; i++) {

			FileStructure sFile = new FileStructure();

			m_xqLog.logDebug("runtimeParamNames : "
					+ runtimeParamNames[i].toString().trim());

			// implemention in version 1.1
			if (defaultProps.getProperty("type.group") != null) {
				sFile.setTypeGroup(defaultProps.getProperty("type.group"));
			}

			m_xqLog.logDebug(".name : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "name"));

			sFile.setName(defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "name")));

			m_xqLog.logDebug(".displayColumn : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "displayColumn"));

			sFile.setDisplayColumn(defaultProps.getProperty(String.format(
					runtimeParam, runtimeParamNames[i].toString().trim(),
					"displayColumn")));

			m_xqLog.logDebug(".startPosition : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "startPosition"));

			sFile.setStartPosition(Integer.parseInt(defaultProps
					.getProperty(String.format(runtimeParam,
							runtimeParamNames[i].toString().trim(),
							"startPosition"))));

			m_xqLog.logDebug(".size : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "size"));

			sFile.setSize(Integer.parseInt(defaultProps.getProperty(String
					.format(runtimeParam, runtimeParamNames[i].toString()
							.trim(), "size"))));

			m_xqLog.logDebug(".type : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "type"));

			sFile.setType(defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "type")));

			m_xqLog.logDebug(".precision : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "precision"));

			if (defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "precision")) != null) {

				sFile.setPrecision(Integer.parseInt(defaultProps
						.getProperty(String.format(runtimeParam,
								runtimeParamNames[i].toString().trim(),
								"precision")).trim()));
			}

			m_xqLog.logDebug(".groupType : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "groupType"));

			if (defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "groupType")) != null) {

				sFile.setGroupType(defaultProps.getProperty(String.format(
						runtimeParam, runtimeParamNames[i].toString().trim(),
						"groupType")));
			}

			m_xqLog.logDebug(".groupStartPosition : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "groupStartPosition"));

			if (defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(),
					"groupStartPosition")) != null) {

				sFile.setGroupStartPosition(Integer.parseInt(defaultProps
						.getProperty(String.format(runtimeParam,
								runtimeParamNames[i].toString().trim(),
								"groupStartPosition"))));
			}

			m_xqLog.logDebug(".groupSize : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "groupSize"));

			if (defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "groupSize")) != null) {

				sFile.setGroupSize(Integer.parseInt(defaultProps
						.getProperty(String.format(runtimeParam,
								runtimeParamNames[i].toString().trim(),
								"groupSize"))));
			}

			m_xqLog.logDebug(".groupValue : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "groupValue"));

			if (defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "groupValue")) != null) {

				sFile.setGroupValue(defaultProps.getProperty(String.format(
						runtimeParam, runtimeParamNames[i].toString().trim(),
						"groupValue")));
			}

			m_xqLog.logDebug("type.groupNames : "
					+ String.format("type.groupNames"));

			if (defaultProps.getProperty("type.groupNames") != null) {

				sFile.setTypeGroupNames(defaultProps
						.getProperty("type.groupNames"));
			}

			m_xqLog.logDebug(".removeTrim : "
					+ String.format(runtimeParam, runtimeParamNames[i]
							.toString().trim(), "removeTrim"));

			if (defaultProps.getProperty(String.format(runtimeParam,
					runtimeParamNames[i].toString().trim(), "removeTrim")) != null) {

				sFile.setRemoveTrim(defaultProps.getProperty(String.format(
						runtimeParam, runtimeParamNames[i].toString().trim(),
						"removeTrim")));
			}

			list.add(sFile);

		}

		is.close();

		return list;
	}

	/**
	 * Handle the arrival of XQMessages in the INBOX.
	 * 
	 * <p>
	 * This method implement a required XQService method.
	 * 
	 * @param ctx
	 *            The service context.
	 * @exception XQServiceException
	 *                Thrown in the event of a processing error.
	 */
	public void service(XQServiceContext ctx) throws XQServiceException {

		if (ctx == null)
			throw new XQServiceException("Service Context cannot be null.");
		else {
			writeParameters(ctx.getParameters());
			convertMessageServiceContext(ctx);
		}

	}

	/**
	 * Clean up and get ready to destroy the service.
	 * 
	 * <p>
	 * This method implement a required XQService method.
	 */
	public void destroy() {
		m_xqLog.logInformation(m_logPrefix + "Destroying...");

		m_xqLog.logInformation(m_logPrefix + "Destroyed...");
	}

	/**
	 * Called by the container on container start.
	 * 
	 * <p>
	 * This method implement a required XQServiceEx method.
	 */
	public void start() {
		m_xqLog.logInformation(m_logPrefix + "Starting...");

		m_xqLog.logInformation(m_logPrefix + "Started...");
	}

	/**
	 * Called by the container on container stop.
	 * 
	 * <p>
	 * This method implement a required XQServiceEx method.
	 */
	public void stop() {
		m_xqLog.logInformation(m_logPrefix + "Stopping...");

		m_xqLog.logInformation(m_logPrefix + "Stopped...");
	}

	/**
	 * Clean up and get ready to destroy the service.
	 * 
	 */
	protected void setLogPrefix(XQParameters params) {
		String serviceName = params.getParameter(
				XQConstants.PARAM_SERVICE_NAME, XQConstants.PARAM_STRING);
		m_logPrefix = "[ " + serviceName + " ]";
	}

	/**
	 * Provide access to the service implemented version.
	 * 
	 */
	protected String getVersion() {
		return s_major + "." + s_minor + ". build " + s_buildNumber;
	}

	/**
	 * Writes a standard service startup message to the log.
	 */
	protected void writeStartupMessage(XQParameters params) {

		final StringBuffer buffer = new StringBuffer();

		String serviceTypeName = params.getParameter(
				XQConstants.SERVICE_PARAM_SERVICE_TYPE,
				XQConstants.PARAM_STRING);

		buffer.append("\n\n");
		buffer.append("\t\t " + serviceTypeName + "\n ");

		buffer.append("\t\t Version ");
		buffer.append(" " + getVersion());
		buffer.append("\n");

		buffer
				.append("\t\t Copyright (c) 2008, Progress Sonic Software Corporation.");
		buffer.append("\n");

		buffer.append("\t\t All rights reserved. ");
		buffer.append("\n");

		m_xqLog.logInformation(buffer.toString());
	}

	/**
	 * Writes parameters to log.
	 */
	protected void writeParameters(XQParameters params) {

		final Map map = params.getAllInfo();
		final Iterator iter = map.values().iterator();

		while (iter.hasNext()) {
			final XQParameterInfo info = (XQParameterInfo) iter.next();

			if (info.getType() == XQConstants.PARAM_XML) {
				m_xqLog.logInformation(m_logPrefix + "Parameter Name =  "
						+ info.getName());
			} else if (info.getType() == XQConstants.PARAM_STRING) {
				m_xqLog.logInformation(m_logPrefix + "Parameter Name = "
						+ info.getName());
			}

			if (info.getRef() != null) {
				m_xqLog.logInformation(m_logPrefix + "Parameter Reference = "
						+ info.getRef());

				// If this is too verbose
				// /then a simple change from logInformation to logDebug
				// will ensure file content is not displayed
				// unless the logging level is set to debug for the ESB
				// Container.
				m_xqLog.logInformation(m_logPrefix
						+ "----Parameter Value Start--------");
				m_xqLog.logInformation("\n" + info.getValue() + "\n");
				m_xqLog.logInformation(m_logPrefix
						+ "----Parameter Value End--------");
			} else {
				m_xqLog.logInformation(m_logPrefix + "Parameter Value = "
						+ info.getValue());
			}
		}
	}
}
