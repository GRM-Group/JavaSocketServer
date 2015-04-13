package pl.grm.sconn;

import java.awt.*;
import java.util.*;
import java.util.concurrent.*;

import pl.grm.sconn.commands.*;
import pl.grm.sconn.connection.*;
import pl.grm.sconn.gui.*;

public class ServerMain extends Observable {
	public static int				EST_PORT				= 4342;
	public static int				START_PORT				= 4343;
	public static int				MAX_PORT				= 4350;
	public static int				CONNECTIONS_MAX_POOL	= 5;
	private ArrayList<Connection>	connectionThreadsList;
	private boolean					running					= false;
	private ExecutorService			executor;
	private Thread					serverConsoleThread;
	private Thread					connectorThread;
	private CommandManager			commandManager;
	private static ServerMain		instance;
	private Connector				connector;
	
	public ServerMain() {
		CLogger.initLogger();
		commandManager = new CommandManager(this);
	}
	
	public static void main(String[] args) {
		Thread.currentThread().setName("Main");
		ServerMain.instance = new ServerMain();
		instance.prepareServer();
		if (args.length != 0 && args[0].equals("gui")) {
			instance.startGUI();
		} else {
			instance.startServer();
		}
	}
	
	private void prepareServer() {
		connectionThreadsList = new ArrayList<Connection>();
		connector = new Connector(this);
		serverConsoleThread = new Thread(new ServerConsole(this));
		serverConsoleThread.start();
	}
	
	public void startServer() {
		if (!isRunning()) {
			CLogger.info("Starting server");
			executor = Executors.newFixedThreadPool(CONNECTIONS_MAX_POOL);
			connectorThread = new Thread(connector);
			connectorThread.start();
			setRunning(true);
			setChanged();
			notifyObservers();
		}
	}
	
	public void stopServer() {
		if (isRunning()) {
			CLogger.info("Stopping server ...\nConnection amount on stop "
					+ connectionThreadsList.size());
			executor.shutdownNow();
			for (Connection connection : connectionThreadsList) {
				connection.closeConnection();
			}
			connectionThreadsList.clear();
			connectorThread.interrupt();
			connectorThread = null;
			executor = null;
			setRunning(false);
			setChanged();
			notifyObservers();
		}
	}
	
	private void startGUI() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ServerGUI sGUI = new ServerGUI(ServerMain.this);
					connector.addObserver(sGUI);
					addObserver(sGUI);
					sGUI.setCommandManager(commandManager);
					sGUI.setVisible(true);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void addNewConnectionThread(Connection connection) {
		connectionThreadsList.add(connection);
	}
	
	public Connection getConnection(int id) {
		if (id < connectionThreadsList.size()) {
			Connection connection = connectionThreadsList.get(id);
			return connection;
		}
		return null;
	}
	
	public boolean executeCommand(String command) {
		return commandManager.executeCommand(command);
	}
	
	public boolean isRunning() {
		return running;
	}
	
	private void setRunning(boolean running) {
		this.running = running;
	}
	
	public int getConnectionsAmount() {
		return connectionThreadsList.size();
	}
}
