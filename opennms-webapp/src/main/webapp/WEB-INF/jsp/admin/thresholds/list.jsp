<%--

/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2007-2008 The OpenNMS Group, Inc.  All rights reserved.
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

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<jsp:include page="/includes/header.jsp" flush="false">
	<jsp:param name="title" value="Thresholds Configuration" />
	<jsp:param name="headTitle" value="List" />
	<jsp:param name="headTitle" value="Thresholds" />
	<jsp:param name="headTitle" value="Admin" />
	<jsp:param name="breadcrumb" value="<a href='admin/index.jsp'>Admin</a>" />
    <jsp:param name="breadcrumb" value="Threshold Groups" />
</jsp:include>

<h3>Threshold Configuration</h3>

<form method="post" name="allGroups">
<table class="standard">
        <tr>
                <th class="standardheader">Name</th>
                <th class="standardheader">RRD Repository</th>
                <th class="standardheader">&nbsp;</th>
        </tr>
        <c:forEach var="mapEntry" items="${groupMap}">
                <tr>
                        <td class="standard">${mapEntry.key}</td>
                        <td class="standard">${mapEntry.value.rrdRepository}</td>
                        <td class="standard"><a href="admin/thresholds/index.htm?groupName=${mapEntry.key}&editGroup">Edit</a></td>
                </tr>
        </c:forEach>
</table>
</form>

<jsp:include page="/includes/footer.jsp" flush="false" />
