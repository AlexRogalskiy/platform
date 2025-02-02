package lsfusion.server.logics.action.session.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import lsfusion.interop.session.remote.RemoteSessionInterface;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.navigator.controller.env.ChangesController;
import lsfusion.server.logics.navigator.controller.env.FormController;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.external.to.ExternalHTTPAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RemoteSession extends RemoteConnection implements RemoteSessionInterface {
    
    private DataSession dataSession;
    
    public RemoteSession(int port, LogicsInstance logicsInstance, AuthenticationToken token, SessionInfo sessionInfo, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, SQLHandledException, InstantiationException, IllegalAccessException {
        super(port, "session", stack);

        setContext(new RemoteSessionContext(this));
        initContext(logicsInstance, token, sessionInfo, stack);

        dataSession = createSession();
    }

    @Override
    public ExternalResponse exec(String actionName, ExternalRequest request) {
        ExternalResponse result;
        try {
            if(actionName != null) {
                LA action;
                String findActionName = actionName;
                while(true) { // we're doing greedy search for all subpathes to find appropriate "endpoint" action
                    int lastSlash = findActionName.lastIndexOf('/'); // if it is url
                    String checkActionName = findActionName;
                    if(lastSlash >= 0) // optimization
                        checkActionName = checkActionName.replace('/', '_');

                    if((action = businessLogics.findActionByCompoundName(checkActionName)) != null || lastSlash < 0)
                        break;

                    findActionName = findActionName.substring(0, lastSlash);
                }
                if (action != null) {
                    result = executeExternal(action, request);
                } else {
                    throw new RuntimeException(String.format("Action %s was not found", actionName));
                }
            } else {
                throw new RuntimeException("Action was not specified");
            }
        } catch (ParseException | SQLHandledException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) {
        ExternalResponse result;
        if (paramScript != null) {
            try {
                Charset charset = Charset.forName(request.charsetName);
                String script = StringClass.text.parseHTTP(paramScript, charset);
                LA<?> runAction = businessLogics.evaluateRun(script, action);
                if(runAction != null) {
                    result = executeExternal(runAction, request);
                } else {
                    throw new RuntimeException("Action with name 'run' was not found");
                }
            } catch (SQLException | ParseException | SQLHandledException | IOException e) {
                throw Throwables.propagate(e);
            }
        } else {
            throw new RuntimeException("Eval script was not found");
        }
        return result;
    }

    private ExternalResponse executeExternal(LA<?> property, ExternalRequest request) throws SQLException, ParseException, SQLHandledException, IOException {
        checkEnableApi(property);

        writeRequestInfo(dataSession, property.action, request);

        property.execute(dataSession, getStack(), ExternalHTTPAction.getParams(dataSession, property, request.params, Charset.forName(request.charsetName)));

        return readResult(request.returnNames, property.action);
    }

    private AuthenticationException authException;
    @Override
    protected void initUser(SecurityManager securityManager, AuthenticationToken token, DataSession session) throws SQLException, SQLHandledException {
        try {
            super.initUser(securityManager, token, session);
        } catch (AuthenticationException e) { // if we have authentication exception, postpone it maybe only noauth will be used (authenticate with anonymous token)
            authException = e;
            super.initUser(securityManager, AuthenticationToken.ANONYMOUS, session);
        }
    }

    @Override
    protected String getCurrentAuthToken() {
        //assert authException == null; // in theory checkEnableApi always should be called first
        return super.getCurrentAuthToken();
    }

    private void checkEnableApi(LA<?> property) {
        boolean forceAPI = false;
        String annotation = property.action.annotation;
        if(annotation != null) {
            if(annotation.equals("noauth"))
                return;
            forceAPI = annotation.equals("api");
        }
        if(authException != null)
            throw authException;        
        checkEnableApi(authToken.isAnonymous(), forceAPI);
    }
    
    private static void checkEnableApi(boolean anonymous, boolean forceAPI) {
        byte enableApi = Settings.get().getEnableAPI();
        if(enableApi == 0) {
            if(forceAPI)
                enableApi = 1;
            else
                throw new RuntimeException("Api is disabled. It can be enabled by using setting enableAPI.");
        }

        if(anonymous && enableApi == 1)
            throw new AuthenticationException();        
    }

    public void writeRequestInfo(DataSession session, Action<?> action, ExternalRequest request) throws SQLException, SQLHandledException {
        if (action.uses(businessLogics.LM.headers.property)) {
            ExternalHTTPAction.writePropertyValues(session, businessLogics.LM.headers, request.headerNames, request.headerValues);
        }
        if (action.uses(businessLogics.LM.cookies.property)) {
            ExternalHTTPAction.writePropertyValues(session, businessLogics.LM.cookies, request.cookieNames, request.cookieValues);
        }
        if (action.uses(businessLogics.LM.query.property)) {
            businessLogics.LM.query.change(request.query, session);
        }
        if (request.query != null && action.uses(businessLogics.LM.params.property)) {
            List<String> paramNames = new ArrayList<>();
            List<String> paramValues = new ArrayList<>();
            for (String param : request.query.split("[&?]")) {
                String[] splittedParam = param.split("=");
                if (splittedParam.length == 2) {
                    paramNames.add(splittedParam[0]);
                    paramValues.add(splittedParam[1]);
                }
            }
            ExternalHTTPAction.writePropertyValues(session, businessLogics.LM.params, paramNames.toArray(new String[0]), paramValues.toArray(new String[0]));
        }
        if (action.uses(businessLogics.LM.contentType.property)) {
            businessLogics.LM.contentType.change(request.contentType, session);
        }
        if (action.uses(businessLogics.LM.body.property)) {
            businessLogics.LM.body.change(new RawFileData(request.body), session);
        }
        if (action.uses(businessLogics.LM.appHost.property)) {
            businessLogics.LM.appHost.change(request.appHost, session);
        }
        if (action.uses(businessLogics.LM.appPort.property)) {
            businessLogics.LM.appPort.change(request.appPort, session);
        }
        if (action.uses(businessLogics.LM.exportName.property)) {
            businessLogics.LM.exportName.change(request.exportName, session);
        }
        if (action.uses(businessLogics.LM.scheme.property)) {
            businessLogics.LM.scheme.change(request.scheme, session);
        }
        if (action.uses(businessLogics.LM.method.property)) {
            businessLogics.LM.method.change(request.method, session);
        }
        if (action.uses(businessLogics.LM.webHost.property)) {
            businessLogics.LM.webHost.change(request.webHost, session);
        }
        if (action.uses(businessLogics.LM.webPort.property)) {
            businessLogics.LM.webPort.change(request.webPort, session);
        }
        if (action.uses(businessLogics.LM.contextPath.property)) {
            businessLogics.LM.contextPath.change(request.contextPath, session);
        }
        if (action.uses(businessLogics.LM.servletPath.property)) {
            businessLogics.LM.servletPath.change(request.servletPath, session);
        }
        if (action.uses(businessLogics.LM.pathInfo.property)) {
            businessLogics.LM.pathInfo.change(request.pathInfo, session);
        }
    }

    private ExternalResponse readResult(String[] returnNames, Action<?> property) throws SQLException, SQLHandledException {
        List<Object> returns = new ArrayList<>();

        LP[] returnProps;
        if (returnNames.length > 0) {
            returnProps = new LP[returnNames.length];
            for (int i = 0; i < returnNames.length; i++) {
                String returnName = returnNames[i];
                LP returnProperty = businessLogics.findPropertyByCompoundName(returnName);
                if (returnProperty == null)
                    throw new RuntimeException(String.format("Return property %s was not found", returnName));
                returnProps[i] = returnProperty;
            }
            for (LP<?> returnProp : returnProps)
                returns.add(formatReturnValue(returnProp.read(dataSession), returnProp.property));
        } else {
            Result<SessionDataProperty> resultProp = new Result<>(); 
            ObjectValue objectValue = businessLogics.LM.getExportValueProperty().readFirstNotNull(dataSession, resultProp, property);
            returns.add(formatReturnValue(objectValue.getValue(), resultProp.result));
        }

        ImOrderMap<String, String> headers = ExternalHTTPAction.readPropertyValues(dataSession, businessLogics.LM.headersTo).toOrderMap();
        String[] headerNames = headers.keyOrderSet().toArray(new String[headers.size()]);
        String[] headerValues = headers.valuesList().toArray(new String[headers.size()]);
        ImOrderMap<String, String> cookies = ExternalHTTPAction.readPropertyValues(dataSession, businessLogics.LM.cookiesTo).toOrderMap();
        String[] cookieNames = cookies.keyOrderSet().toArray(new String[cookies.size()]);
        String[] cookieValues = cookies.valuesList().toArray(new String[cookies.size()]);

        Integer statusHttp = (Integer) businessLogics.LM.statusHttpTo.read(dataSession);

        return new ExternalResponse(returns.toArray(), headerNames, headerValues, cookieNames, cookieValues, statusHttp);
    }

    private Object formatReturnValue(Object returnValue, Property returnProperty) {
        Type returnType = returnProperty.getType();
        return returnType.formatHTTP(returnValue, null);
    }

    @Override
    protected FormController createFormController() {
        return new FormController() {
            @Override
            public void changeCurrentForm(String form) {
                throw new RuntimeException("not supported");
            }

            @Override
            public String getCurrentForm() {
                return null;
            }
        };
    }

    @Override
    protected ChangesController createChangesController() {
        return new ChangesController() {
            protected DBManager getDbManager() {
                return dbManager;
            }
        };
    }

    @Override
    protected Long getConnectionId() {
        return null;
    }

    @Override
    public Object getProfiledObject() {
        return "rs";
    }

    @Override
    protected void onClose() {
        try {
            dataSession.close();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
        
        super.onClose();
    }
}
