<%@page contentType="text/html;charset=UTF-8"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<c:set var="head" value="<meta http-equiv='refresh' content='10'><title>(${numberOfTasks}) SK - status</title>
	<style type='text/css'>.run {color:red}</style>" />
<%@ include file="common-top.jsp"%>


<h3>Tasks in progress or ready to be processed (${fn:length(tasksInRun)})</h3>

<c:if test="${fn:length(tasksInRun) > 0}">
	<ul class="monospace">
		<c:forEach items="${tasksInRun}" var="task">
			<li
				${task.processingStarted ? ' class="run"' : ''}>${task}
			</li>
		</c:forEach>
	</ul>
</c:if>
<c:if test="${fn:length(tasksInRun) == 0}">
(empty)
</c:if>

<h3>Tasks in queue with not satisfied preconditions (${fn:length(tasksInQueue)})</h3>

<c:if test="${fn:length(tasksInQueue) > 0}">
<ul class="monospace">
	<c:forEach items="${tasksInQueue}" var="task">
		<li${task.processingStarted ? 'class="run"' : ''}>
			${task}
		</li>
	</c:forEach>
</ul>
</c:if>
<c:if test="${fn:length(tasksInQueue) == 0}">
(empty)
</c:if>

<%@ include file="common-bottom.jsp"%>
