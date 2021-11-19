<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns:th="https://thymeleaf.org">    
    <form action="/query" modelAttribute="queryObject">
        <table>
            <tr>
                <label for="query">Please input your query:</label>
                <input type="text" id="query" name="query"><br>
            </tr>
            <tr>
                <select name="type">
                    <option value="0" selected>Disjunctive</option>
                    <option value="1">Conjunctive</option>
                </select>
            </tr>
            <tr>
                <td><input type="submit" value="Submit"/></td>
            </tr>
        </table>
    </form>
    <c:forEach items="${responseList}" var="response">
        <c:forEach items="${response.termParametersMap}" var="entry">
            Term = ${entry.key}, length of list for the term = ${entry.value[0]}, frequency of term in document = ${entry.value[1]}, length of page = ${entry.value[2]}
            <br>
        </c:forEach>
        BM Score:<c:out value="${response.score}"/><br>
        <c:out value="${response.snippet}"/>
        <br>
        <br>
    </c:forEach>
</html>  