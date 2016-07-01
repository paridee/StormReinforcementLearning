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

}
