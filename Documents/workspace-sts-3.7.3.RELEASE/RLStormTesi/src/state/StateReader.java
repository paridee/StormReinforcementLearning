package state;

public interface StateReader {
	int getCurrentState();
	boolean isOperatorUnderloaded(String opName);
}
