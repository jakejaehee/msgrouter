package msgrouter.engine;

public interface ThreadInitializer {

	public void setServiceClassLoader(ClassLoader cl);

	public void execute() throws Exception;
}
