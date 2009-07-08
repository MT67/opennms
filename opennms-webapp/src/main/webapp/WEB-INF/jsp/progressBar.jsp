<%--

/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2006-2008 The OpenNMS Group, Inc.  All rights reserved.
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

<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%

/*
 * Hopefully our servlet container will not have flushed the few newlines of
 * output from above before we set the headers.
 */

long startTime = System.currentTimeMillis();
response.setHeader("Refresh", "2");
response.setHeader("Cache-Control", "no-store, private");
response.setDateHeader("Date", startTime);
response.setDateHeader("Expires", startTime);

%>
 
<jsp:include page="/includes/header.jsp" flush="false">
	<jsp:param name="title" value="Progress" />
	<jsp:param name="headTitle" value="Progress" />
</jsp:include>

<c:set var="label" value="${progress.phaseLabel}"/>
<c:set var="percentage">
	<fmt:formatNumber maxFractionDigits="0" value="${progress.phase / progress.phaseCount * 100}"/>
</c:set>

  <div align="center">
    <p style="margin-bottom: 0px; font-size: 80%;">
      ${label}...
    </p>

    <div style="width: 400px; height: 25px; border-size: 1px; border-style: ridge; background-color: white;">
      <div style="float: left; width: ${percentage}%; height: 25px; background-color: green;">&nbsp;</div>
    </div>
    
    <p>
      ${percentage}% completed
    </p>
  </div>

<jsp:include page="/includes/footer.jsp" flush="false" />

