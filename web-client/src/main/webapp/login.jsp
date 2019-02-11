<%@ page import="lsfusion.base.ServerMessages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>lsFusion</title>

        <link rel="stylesheet" href="login.css">
        <link rel="shortcut icon" href="favicon.ico" />
    </head>
    <body onload="document.loginForm.j_username.focus();">
        <div style="visibility: hidden;">448b0ce6-206e-11e9-ab14-d663bd873d93</div>
        <table class="content-table">
            <tr></tr>
            <tr>
                <td>
                    <div id="content">

                        <%
                            String query = request.getQueryString();
                            String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                        %>

                        <form id="login-form"
                              name="loginForm"
                              method="POST"
                              action="login_check<%=queryString%>" >
                            <fieldset>

                                <div class="image-center"><img src="readLogo<%=queryString%>" alt="LSFusion"></div>
                                <p>
                                    <br/>
                                    <label for="j_username"><%= ServerMessages.getString(request, "login") %></label>
                                    <input type="text" id="j_username" name="j_username" class="round full-width-input"/>
                                </p>
                                <p>
                                    <label for="j_password"><%= ServerMessages.getString(request, "password") %></label>
                                    <input type="password" id="j_password" name="j_password" class="round full-width-input"/>
                                </p>
                                <input name="submit" type="submit" class="button round blue image-right ic-right-arrow" value="<%= ServerMessages.getString(request, "log.in") %>"/>
                                <div class="desktop-link">
                                    <span id="triangle" class="triangle" onclick="showSpoiler()">&#9658;</span><a href="${pageContext.request.contextPath}/client.jnlp<%=queryString%>"><%= ServerMessages.getString(request, "run.desktop.client") %></a>
                                    <div id="spoiler" style="display:none"></div>
                                        <script>
                                            function showSpoiler() {
                                                if(document.getElementById('spoiler').style.display==='none') {

                                                    var xhttp = new XMLHttpRequest();
                                                    xhttp.onload = function() {
                                                        document.getElementById('spoiler').innerHTML = this.responseText.split("{contextPath}").join("${pageContext.request.contextPath}");
                                                    };
                                                    xhttp.open("GET", "exec?action=Security.generateJnlpUrls%5B%5D&return=Security.jnlpUrls%5B%5D", true);                                                    
                                                    xhttp.send();
                                                    document.getElementById('spoiler') .style.display='';
                                                    document.getElementById('triangle').innerHTML = '&#9660;'
                                                } else {
                                                    document.getElementById('spoiler') .style.display='none';
                                                    document.getElementById('triangle').innerHTML = '&#9658;'
                                                }
                                            }
                                    </script>
                                </div>
                            </fieldset>
                        </form>
                        <c:if test="${!empty param.error}">
                            <div class="errorblock round">
                                <%= ServerMessages.getString(request, "login.unsuccessful") %><br/>
                                <%= ServerMessages.getString(request, "login.caused") %>: ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
                            </div>
                        </c:if>
                    </div>
                </td>
            </tr>
            <tr></tr>
        </table>

</body>
</html>