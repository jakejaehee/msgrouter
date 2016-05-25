package msgrouter.engine.config;

public class Config {
	public static final boolean IS_WINDOWS = isWindows();

	private MainConfig mainConfig;
	private ContainersConfig containersConfig;

	public void setMainConfig(MainConfig mainConfig) {
		this.mainConfig = mainConfig;
	}

	public MainConfig getMainConfig() {
		return mainConfig;
	}

	public void setContainersConfig(ContainersConfig containersConfig) {
		this.containersConfig = containersConfig;
	}

	public ContainersConfig getContainersConfig() {
		return containersConfig;
	}

	private static final boolean isWindows() {
		return System.getProperty("os.name").indexOf("Windows") != -1;
	}
}
