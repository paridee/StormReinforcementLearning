package mainClasses;

import org.eclipse.jetty.util.log.Logger;

public class NoLogging implements Logger {

	@Override
	public void debug(Throwable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(String arg0, Object... arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Logger getLogger(String arg0) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void ignore(Throwable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void info(Throwable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void info(String arg0, Object... arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void info(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDebugEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDebugEnabled(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void warn(Throwable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void warn(String arg0, Object... arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void warn(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

}
