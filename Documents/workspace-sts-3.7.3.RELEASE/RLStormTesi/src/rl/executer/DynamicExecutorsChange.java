package rl.executer;

//TODO FARE CON CAMBIO COSTANTE up-down


public class DynamicExecutorsChange implements ActionExecutor {
	private int state	=	-1;
	
	@Override
	public double execute(int action) {
		if(this.state==-1){
			try {
				throw new Exception("missing parameters (system state to be set)");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void setState(int state){
		this.state	=	state;
	}

	@Override
	public double execute(int action, int state) {
		// TODO Auto-generated method stub
		return execute(action);
	}

	@Override
	public boolean isFeasible(int currentState, int i) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] newConfigurationPreview(int action, int state) {
		// TODO Auto-generated method stub
		return null;
	}

}
