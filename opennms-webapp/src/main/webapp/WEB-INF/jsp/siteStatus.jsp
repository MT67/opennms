<%--

/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2006-2009 The OpenNMS Group, Inc.  All rights reserved.
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

<jsp:include page="/includes/header.jsp" flush="false">
	<jsp:param name="title" value="Site Status Page" />
	<jsp:param name="headTitle" value="Site Status" />
	<jsp:param name="breadcrumb" value="Site Status" />
	<jsp:param name="breadcrumb" value="${view.columnValue}" />
</jsp:include>

<h3>Site status for nodes in site '${view.columnValue}'</h3>

  <table>
    <thead>
      <tr>
        <th>Device Type</th>
        <th>Nodes Down</th>
      </tr>
    </thead>
    <c:forEach items="${stati}" var="status">
      <tr class="CellStatus" >
        <td>${status.label}</td>
        <td class="${status.status} divider" >
          <c:choose>
            <c:when test="${! empty status.link}">
              <c:url var="statusLink" value="${status.link}"/>
              <a href="${statusLink}">${status.downEntityCount} of ${status.totalEntityCount}</a>
            </c:when>
            <c:otherwise>
              ${status.downEntityCount} of ${status.totalEntityCount}
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:forEach>
  </table>
  
<h3>Site outages</h3>
<div class="boxWrapper">
  <c:url var="outagesLink" value="outage/list.htm">
    <c:param name="filter" value="building=${view.columnValue}"/>
  </c:url>
  <p>
    <a href="${outagesLink}">View</a> current site outages.
  </p>
</div>

<jsp:include page="/includes/footer.jsp" flush="false" />
