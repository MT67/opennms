<%--

/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2002-2006, 2008 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc.:
 *
 *      51 Franklin Street
 *      5th Floor
 *      Boston, MA 02110-1301
 *      USA
 *
 * For more information contact:
 *
 *      OpenNMS Licensing <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *
 *******************************************************************************/


--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	isErrorPage="true"
%>

<%
    SecurityException e;

    if( exception instanceof ServletException ) {
        e = (SecurityException)((ServletException)exception).getRootCause();
    }
    else { 
        e = (SecurityException)exception;
    }

    if( !e.getMessage().equals( "sealing violation" )) {
        throw new ServletException( "security exception", e );
    }
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Error" />
  <jsp:param name="headTitle" value="Incorrect Jar Files" />
  <jsp:param name="headTitle" value="Error" />
  <jsp:param name="breadcrumb" value="Error" />
</jsp:include>

<h1>Incorrect Jar Files</h1>

<p>
  Some of the Java Archive files (jar files) in the Tomcat install
  are out of date.  Please replace them by going to this      
  <a href="http://faq.opennms.org/fom-serve/cache/55.html">OpenNMS FAQ
  entry</a> and following the directions there.  Otherwise, your OpenNMS
  Web system will not work correctly, and you will get undefined results.
</p>

<jsp:include page="/includes/footer.jsp" flush="false" />
