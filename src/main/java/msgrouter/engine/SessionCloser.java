package msgrouter.engine;

public interface SessionCloser {
	public void setService(Service service) throws Exception;

	public void setSession(Session session) throws Exception;

	public void execute() throws Exception;
}
