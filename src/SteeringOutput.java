import java.util.Vector;


public class SteeringOutput<E> {
	
	private Vector<E> linear;
	private double angular;
	
	public SteeringOutput() {
	}
	
	public Vector<E> getLinear() {
		return linear;
	}
	public void setLinear(Vector<E> linear) {
		this.linear = linear;
	}
	public double getAngular() {
		return angular;
	}
	public void setAngular(double angular) {
		this.angular = angular;
	}
}
