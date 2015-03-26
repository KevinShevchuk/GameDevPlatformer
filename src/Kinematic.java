import java.util.Vector;


public class Kinematic<E> {
	private Vector<E> position;
	private double orientation;
	private Vector<E> velocity;
	private double rotation;
	private SteeringOutput so;
	
	public Kinematic(Vector<E> startpos, double orientation) {
		this.position = startpos;
		this.orientation = orientation;
		this.so = new SteeringOutput();
	}
	
	public Vector<E> getPosition() {
		return position;
	}
	public void setPosition(Vector<E> position) {
		this.position = position;
	}
	public double getOrientation() {
		return orientation;
	}
	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	public Vector<E> getVelocity() {
		return velocity;
	}
	public void setVelocity(Vector<E> velocity) {
		this.velocity = velocity;
	}
	public double getRotation() {
		return rotation;
	}
	public void setRotation(double rotation) {
		this.rotation = rotation;
	}
	
	public void update(SteeringOutput so, double time) {
		//Update Position and orientation
		position += velocity * time;
		
	}
}
