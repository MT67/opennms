//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//

package org.opennms.web.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.opennms.core.resource.Vault;
import org.opennms.core.utils.StreamUtils;
import org.opennms.netmgt.rrd.RrdException;
import org.opennms.netmgt.rrd.RrdUtils;
import org.opennms.netmgt.utils.RrdFileConstants;
import org.opennms.web.MissingParameterException;

/**
 * A servlet that creates a graph of network performance data using the <a
 * href="http://www.rrdtool.org/">RRDTool </a>.
 * 
 * <p>
 * This servlet executes an <em>rrdtool graph</em> command in another process,
 * piping its PNG file to standard out. The servlet then reads that PNG file and
 * returns it on the <code>ServletOutputStream</code>.
 * </p>
 * 
 * <p>
 * This servlet requires the following parameters:
 * <ul>
 * <li><em>report</em> The name of the key in the rrdtool-graph properties
 * file that contains information (including the command line options) to
 * execute specific graph query.
 * <li><em>rrd</em> The name of the ".rrd" file to graph. The file must exist
 * in the input directory specified in the rrdtool-graph properties file.
 * <li><em>start</em> The start time.
 * <li><em>end</em> The end time.
 * </ul>
 * </p>
 * 
 * @author <A HREF="mailto:larry@opennms.org">Lawrence Karnowski </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
public class RRDGraphServlet extends HttpServlet {
    /**
    * 
    */
    private static final long serialVersionUID = 8890231247851529359L;

    private String s_initParam = "rrd-properties";
    private String s_missingParamsPath = "/images/rrd/missingparams.png";
    private String s_rrdError = "/images/rrd/error.png";

    /** 
     * Holds the GraphTypeConfig for all supported graph types.  It maps
     * graph types to {@link GraphTypeConfig GraphTypeconfig} instances.
     */
    private Map m_graphTypeMap = new HashMap();

    /**
     * Initializes this servlet by reading the rrdtool-graph properties file.
     */
    public void init() throws ServletException {
        String homeDir = Vault.getHomeDir();
        String configs = getServletConfig().getInitParameter(s_initParam);
        
        try {
            String[] configEntries = configs.split(";");
            for (int i = 0; i < configEntries.length; i++) {
                String[] entry = configEntries[i].split("=");
                if (entry.length != 2) {
                    throw new ServletException("Incorrect number of equals "
                            + "signs in servlet init "
                            + "parameter \""
                            + s_initParam
                            + "\": " + configs);
                }
                String type = entry[0];
                String configFile = entry[1];
                
                GraphTypeConfig config =
                    loadGraphTypeConfig(type, homeDir + configFile);
                m_graphTypeMap.put(type, config);
            }
        } catch (PatternSyntaxException e) {
            String message = "Could not parse servlet init parameter \""
                + s_initParam + "\"";
            log(message, e);
            throw new ServletException(message, e);
        }
    }

    public GraphTypeConfig loadGraphTypeConfig(String type,
					       String propertiesFilename)
		throws ServletException {
        Properties properties = new Properties();

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(propertiesFilename);
            properties.load(fileInputStream);

            RrdUtils.graphicsInitialize();
        } catch (FileNotFoundException e) {
            log("Could not find configuration file", e);
            throw new ServletException("Could not find configuration file", e);
        } catch (IOException e) {
            log("Could not load configuration file", e);
            throw new ServletException("Could not load configuration file: ", e);
        } catch (RrdException e) {
            log("Could not inititalize the graphing system", e);
            throw new ServletException("Could not initialize graphing system: " + e.getMessage(), e);
        } catch (Throwable e) {
            log("Unexpected exception or error occurred", e);
            throw new ServletException("Unexpected exception or error occured: " + e.getMessage(), e);
        } finally {
            try {
                if (fileInputStream != null) fileInputStream.close();
            } catch (Exception e) {
                this.log("init: Error closing properties file.",e);
            }
        }

        GraphTypeConfig config = new GraphTypeConfig();

        config.setWorkDir(new File(properties.getProperty("command.input.dir")));
        config.setCommandPrefix(properties.getProperty("command.prefix"));
        config.setMimeType(properties.getProperty("output.mime"));

	Map map;
	if (isTypeAdHoc(type)) {
	    map = new HashMap();
	    map.put("adhoc", properties);
	} else {
	    map = PrefabGraph.getPrefabGraphDefinitions(properties);
	}
        config.setReportMap(map);

        return config;
    }

    public GraphTypeConfig getGraphConfig(String type) throws ServletException {
	GraphTypeConfig config = (GraphTypeConfig) m_graphTypeMap.get(type);

        if (config == null) {
	    throw new ServletException("Invalid graph type \"" + type + "\"");
	}

	return config;
    }

    /**
     * Checks the parameters passed to this servlet, and if all are okay,
     * executes the RRDTool command in another process and pipes its PNG output
     * to the <code>ServletOutputStream</code> back to the requesting web
     * browser.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
	    throws ServletException, IOException {
        String type = request.getParameter("type");

        if (type == null) {
	    returnErrorImage(response, s_missingParamsPath);
            return;
        }

	GraphTypeConfig config = getGraphConfig(type);

	String command;
	if (isTypeAdHoc(type)) {
	    command = getCommandAdhoc(config, request, response);
	} else {
	    command = getCommandNonAdhoc(config, request, response);
	}

	if (command == null) {
	    returnErrorImage(response, s_missingParamsPath);
            return;
        }

        File workDir = config.getWorkDir();
        InputStream tempIn = null;
        try {
            log("Executing RRD command in this directory: " + workDir);
            log(command);

            tempIn = RrdUtils.createGraph(command, workDir);
        } catch (RrdException e) {
            log("Read from stderr: " + e.getMessage());
	    returnErrorImage(response, s_rrdError);
	    return;
	}

        if (tempIn != null) {
            response.setContentType(config.getMimeType());

            StreamUtils.streamToStream(tempIn, response.getOutputStream());

            tempIn.close();
        }
    }

    public String getCommandNonAdhoc(GraphTypeConfig config,
            HttpServletRequest request,
            HttpServletResponse response)
    throws ServletException {
        String report = request.getParameter("report");
        String[] rrds = request.getParameterValues("rrd");
        String propertiesFile = request.getParameter("props");
        String start = request.getParameter("start");
        String end = request.getParameter("end");
        
        if (report == null || rrds == null || start == null || end == null) {
            return null;
        }
        
        for (int i = 0; i < rrds.length; i++) {
            if (!RrdFileConstants.isValidRRDName(rrds[i])) {
                log("Illegal RRD filename: " + rrds[i]);
                throw new IllegalArgumentException("Illegal RRD filename: "
                        + rrds[i]);
            }
        }
        
        return createPrefabCommand(request,
                config.getReportMap(),
                config.getCommandPrefix(),
                config.getWorkDir(), report, rrds,
                propertiesFile,
                start, end);
    }
    
    protected String createPrefabCommand(HttpServletRequest request,
            Map reportMap, String commandPrefix,
            File workDir, String reportName,
            String[] rrds, String propertiesFile,
            String start, String end)
    throws ServletException {
        PrefabGraph graph = (PrefabGraph) reportMap.get(reportName);
        
        if (graph == null) {
            throw new IllegalArgumentException("Unknown report name: "
                    + reportName);
        }
        
        StringBuffer buf = new StringBuffer();
        buf.append(commandPrefix);
        buf.append(" ");
        buf.append(graph.getCommand());
        String command = buf.toString();
        
        long startTime = Long.parseLong(start);
        long endTime = Long.parseLong(end);
        long diffTime = endTime - startTime;
        
        String startTimeString = Long.toString(startTime / 1000);
        String endTimeString = Long.toString(endTime / 1000);
        String diffTimeString = Long.toString(diffTime / 1000);
        
        /*
         // remember rrdtool wants the time in seconds, not milliseconds;
          // java.util.Date.getTime() returns milliseconds, so divide by 1000
           String starttime = Long.toString(Long.parseLong(start) / 1000);
           String endtime = Long.toString(Long.parseLong(end) / 1000);
           */
        
        HashMap translationMap = new HashMap();
        
        for (int i = 0; i < rrds.length; i++) {
            String key = "{rrd" + (i + 1) + "}";
            translationMap.put(RE.simplePatternToFullRegularExpression(key),
                    rrds[i]);
        }
        
        translationMap.put(RE.simplePatternToFullRegularExpression("{startTime}"), startTimeString);
        translationMap.put(RE.simplePatternToFullRegularExpression("{endTime}"), endTimeString);
        translationMap.put(RE.simplePatternToFullRegularExpression("{diffTime}"), diffTimeString);
        
        Properties externalProperties = loadExternalProperties(workDir, propertiesFile);
        
        // names of values specified outside of the RRD data (external values)
        String[] externalValues = graph.getExternalValues();
        
        if (externalValues != null || externalValues.length > 0) {
            for (int i = 0; i < externalValues.length; i++) {
                String value = request.getParameter(externalValues[i]);
                
                if (value == null) {
                    throw new MissingParameterException(externalValues[i]);
                } else {
                    translationMap.put(RE.simplePatternToFullRegularExpression("{" + externalValues[i] + "}"), value);
                }
            }
        }
        
        //names of values specified that come from properties files
        String[] propertiesValues = graph.getPropertiesValues();
        if (propertiesValues != null || propertiesValues.length > 0) {
            for (int i = 0; i < propertiesValues.length; i++) {
                String value = (externalProperties.getProperty(propertiesValues[i]) == null ? "Unknown" : externalProperties.getProperty(propertiesValues[i]));
                if (value == null) {
                    throw new MissingParameterException(propertiesValues[i]);
                } else {
                    translationMap.put(
                            RE.simplePatternToFullRegularExpression(
                                    "{" + propertiesValues[i] + "}"),
                                    value);
                }
            }
        }
        
        
        try {
            Iterator iter = translationMap.keySet().iterator();
            
            while (iter.hasNext()) {
                String s1 = (String) iter.next();
                String s2 = (String) translationMap.get(s1);
                
                // replace s1 with s2
                RE re = new RE(s1);
                command = re.subst(command, s2);
            }
        } catch (RESyntaxException e) {
            throw new ServletException("Invalid regular expression syntax, check rrd-properties file", e);
        }
        
        return command;
    }

    public Properties loadExternalProperties(File workDir, String propertiesFile) {
    	Properties externalProperties = new Properties();
    	
    	if (propertiesFile == null) {
    		return externalProperties;
    	}
    	
    	File file = new File(workDir, propertiesFile);
    	if (!file.exists()) {
    		log("loadExternalProperties: Properties file does not exist: " + file.getAbsolutePath());
    		return externalProperties;
    	}
    	
    	FileInputStream fileInputStream = null;
    	try {
    		fileInputStream = new FileInputStream(file);
    	} catch (Exception e) {
    		log("createPrefabGraph: Error opening properties file: "+propertiesFile, e);
    		return externalProperties;
    	}

   		try {
			externalProperties.load(fileInputStream);
		} catch (IOException e) {
    		log("createPrefabGraph: Error loading properties file: "+propertiesFile, e);
		} finally {
            try {
                if (fileInputStream != null) {
                	fileInputStream.close();
                }
            } catch (Exception e) {
                this.log("createPrefabGraph: Error closing properties file: "+propertiesFile, e);
            }      
        }
    		
    	return externalProperties;
    }
    


    public String getCommandAdhoc(GraphTypeConfig config,
			          HttpServletRequest request,
			          HttpServletResponse response)
		throws ServletException {
	String rrdDir = request.getParameter("rrddir");
        String start = request.getParameter("start");
        String end = request.getParameter("end");

        if (rrdDir == null || start == null || end == null) {
            return null;
        }

        if (!RrdFileConstants.isValidRRDName(rrdDir)) {
            log("Illegal RRD directory: " + rrdDir);
            throw new IllegalArgumentException("Illegal RRD directory: " + rrdDir);
        }

        return createAdHocCommand(request, config, rrdDir, start, end);
    }

    protected String createAdHocCommand(HttpServletRequest request,
					GraphTypeConfig config,
					String rrdDir,
					String start, String end) {
	Properties properties =
	    (Properties) config.getReportMap().get("adhoc");
	String commandPrefix = config.getCommandPrefix();

        String title = properties.getProperty("adhoc.command.title");
        String ds = properties.getProperty("adhoc.command.ds");
        String graphline = properties.getProperty("adhoc.command.graphline");

        // remember rrdtool wants the time in seconds, not milliseconds;
        // java.util.Date.getTime() returns milliseconds, so divide by 1000
        String starttime = Long.toString(Long.parseLong(start) / 1000);
        String endtime = Long.toString(Long.parseLong(end) / 1000);

        String graphtitle = request.getParameter("title");

        if (graphtitle == null) {
            return null;
        }

        StringBuffer buf = new StringBuffer();
        buf.append(commandPrefix);
        buf.append(" ");
        buf.append(title);

        String dsNames[] = request.getParameterValues("ds");
        String dsAggregFxns[] = request.getParameterValues("agfunction");
        String colors[] = request.getParameterValues("color");
        String dsTitles[] = request.getParameterValues("dstitle");
        String dsStyles[] = request.getParameterValues("style");

        if (dsNames == null || dsAggregFxns == null || colors == null ||
	    dsTitles == null || dsStyles == null) {
            return null;
        }

        for (int i = 0; i < dsNames.length; i++) {
            String dsAbbrev = "ds" + Integer.toString(i);

            String dsName = dsNames[i];
            String rrd = rrdDir + File.separator + dsNames[i]
			 + RrdFileConstants.RRD_SUFFIX;
            String dsAggregFxn = dsAggregFxns[i];
            String color = colors[i];
            String dsTitle = dsTitles[i];
            String dsStyle = dsStyles[i];

            buf.append(" ");
            buf.append(MessageFormat.format(ds, new String[] { rrd,
							       starttime,
							       endtime,
							       graphtitle,
							       dsAbbrev,
							       dsName,
							       dsAggregFxn,
							       dsStyle,
							       color,
							       dsTitle }));
        }

        for (int i = 0; i < dsNames.length; i++) {
            String dsAbbrev = "ds" + Integer.toString(i);

            String dsName = dsNames[i];
            String rrd = rrdDir + File.separator + dsNames[i]
			 + RrdFileConstants.RRD_SUFFIX;
            String dsAggregFxn = dsAggregFxns[i];
            String color = colors[i];
            String dsTitle = dsTitles[i];
            String dsStyle = dsStyles[i];

            buf.append(" ");
            buf.append(MessageFormat.format(graphline,
					    new String[] { rrd,
							   starttime,
							   endtime,
							   graphtitle,
							   dsAbbrev,
							   dsName,
							   dsAggregFxn,
							   dsStyle,
							   color,
							   dsTitle }));
        }

        return MessageFormat.format(buf.toString(),
				    new String[] { "bogus-rrd",
						   starttime,
						   endtime,
						   graphtitle });
    }

    public boolean isTypeAdHoc(String type) {
	return type.endsWith("-adhoc");
    }

    public void returnErrorImage(HttpServletResponse response, String file)
	    throws IOException {
	response.setContentType("image/png");
	InputStream is =
	    getServletContext().getResourceAsStream(file);
	StreamUtils.streamToStream(is, response.getOutputStream());
    }

    public class GraphTypeConfig {
        /**
         * The working directory as specifed in the rrdtool-graph properties
	 * file.
         */
        private File m_workDir;
    
        /**
         * The prefix for the RRDtool command (including the executable's
         * pathname) as specified in the rrdtool-graph properties file.
         */
        private String m_commandPrefix;
    
        /**
         * The mime type of the image we will return.
         */
        private String m_mimeType;
    
        /**
         * Holds the graph definitions specified in the rrdtool-graph
         * properties file. It maps report names to {@link PrefabGraph
         * PrefabGraph} instances.
         */
        private Map m_reportMap;


        public void setWorkDir(File workDir) {
            m_workDir = workDir;
        }

        public File getWorkDir() {
            return m_workDir;
        }

        public void setCommandPrefix(String commandPrefix) {
            m_commandPrefix = commandPrefix;
        }

        public String getCommandPrefix() {
            return m_commandPrefix;
        }

        public void setMimeType(String mimeType) {
            m_mimeType = mimeType;
        }

        public String getMimeType() {
            return m_mimeType;
        }

        public void setReportMap(Map reportMap) {
            m_reportMap = reportMap;
        }

        public Map getReportMap() {
            return m_reportMap;
        }
    }
}
